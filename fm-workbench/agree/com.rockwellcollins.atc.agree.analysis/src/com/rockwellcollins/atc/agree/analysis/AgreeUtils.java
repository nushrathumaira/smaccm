package com.rockwellcollins.atc.agree.analysis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jkind.lustre.BoolExpr;
import jkind.lustre.EnumType;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.RealExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordType;
import jkind.lustre.Type;
import jkind.lustre.TypeDef;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.xtext.ui.editor.GlobalURIEditorOpener;
import org.osate.aadl2.Aadl2Factory;
import org.osate.aadl2.AbstractNamedValue;
import org.osate.aadl2.BusType;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.ContainedNamedElement;
import org.osate.aadl2.ContainmentPathElement;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DataType;
import org.osate.aadl2.DeviceType;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.ProcessType;
import org.osate.aadl2.ProcessorType;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyConstant;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.SubprogramType;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.ThreadGroupType;
import org.osate.aadl2.ThreadType;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.properties.PropertyDoesNotApplyToHolderException;
import org.osate.aadl2.properties.PropertyNotPresentException;
import org.osate.annexsupport.AnnexUtil;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;

import com.google.inject.Injector;
import com.rockwellcollins.atc.agree.agree.AgreePackage;
import com.rockwellcollins.atc.agree.agree.AssumeStatement;
import com.rockwellcollins.atc.agree.agree.EqStatement;
import com.rockwellcollins.atc.agree.agree.FnCallExpr;
import com.rockwellcollins.atc.agree.agree.GuaranteeStatement;
import com.rockwellcollins.atc.agree.agree.NestedDotID;
import com.rockwellcollins.atc.agree.agree.PropertyStatement;
import com.rockwellcollins.atc.agree.analysis.ast.AgreeProgram;
import com.rockwellcollins.atc.agree.analysis.ast.AgreeStatement;
import com.rockwellcollins.atc.agree.analysis.preferences.PreferenceConstants;
import com.rockwellcollins.atc.agree.ui.internal.AgreeActivator;

public class AgreeUtils {
    
    private static GlobalURIEditorOpener globalURIEditorOpener;

    static public boolean usingKind2() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        String solver = prefs.getString(PreferenceConstants.PREF_MODEL_CHECKER);

        return solver.equals(PreferenceConstants.MODEL_CHECKER_KIND2)
                || solver.equals(PreferenceConstants.MODEL_CHECKER_KIND2WEB);
    }
    
    static public PropertyExpression getPropExpression(NamedElement comp, Property prop) {

        PropertyExpression expr;
        try {
            comp.getPropertyValue(prop); // this just checks to see if the
            // property is associated
            expr = PropertyUtils.getSimplePropertyValue(comp, prop);
            return expr;
        } catch (PropertyDoesNotApplyToHolderException propException) {
            return null;
        } catch (PropertyNotPresentException propNotPresentException) {
            return null;
        }
    }

    public static NamedElement getFinalNestId(NestedDotID dotId) {
        while (dotId.getSub() != null) {
            dotId = dotId.getSub();
        }
        return dotId.getBase();
    }

    public static ComponentImplementation getInstanceImplementation(ComponentInstance compInst) {
        if (compInst instanceof SystemInstance) {
            ComponentImplementation ci;
            ci = ((SystemInstance) compInst).getComponentImplementation();
            return ci;
        }
        try {
            return (ComponentImplementation) compInst.getComponentClassifier();
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static ComponentType getInstanceType(ComponentInstance compInst) {
        if (compInst instanceof SystemInstance) {

            ComponentType ct;
            ct = ((SystemInstance) compInst).getComponentImplementation().getType();
            return ct;

        }
        try {
            return ((ComponentImplementation) compInst.getComponentClassifier()).getType();
        } catch (ClassCastException e) {
            return (ComponentType) compInst.getComponentClassifier();
        }
    }

    // warns the user if there is a cycle
    static public void logCycleWarning(List<Equation> eqs, AgreeRenaming agreeRename,
            boolean throwException) {
        Map<String, Set<String>> idGraph = new HashMap<>();
        List<String> ids = new ArrayList<>();
        AgreeCycleVisitor visitor = new AgreeCycleVisitor();

        for (Equation eq : eqs) {
            for (IdExpr id : eq.lhs) {
                ids.add(id.id);
                idGraph.put(id.id, eq.expr.accept(visitor));
            }
        }

        Set<String> discovered = new HashSet<>();

        StringBuilder exceptionStr = new StringBuilder();
        for (String id : ids) {
            if (discovered.contains(id)) {
                continue;
            }
            List<String> cycle = cycleWarning_Helper(id, new HashSet<String>(), idGraph);

            if (cycle != null) {
                discovered.addAll(cycle);
                String aadlString = agreeRename.rename(id);
                StringBuilder cycleStr = new StringBuilder("Possible cycle: " + aadlString);

                String sep = " -> ";
                for (String cycleId : cycle) {
                    cycleStr.append(sep);
                    aadlString = agreeRename.rename(cycleId);
                    cycleStr.append(aadlString);
                }

                AgreeLogger.logWarning(cycleStr.toString());
                exceptionStr.append(cycleStr);
            }
        }
        if (throwException && !discovered.isEmpty()) {
            throw new AgreeCombinationalCycleException(exceptionStr.toString());
        }
    }

    static public LinkedList<String> cycleWarning_Helper(String visit, Set<String> visited,
            Map<String, Set<String>> graph) {

        if (visited.contains(visit)) {
            return null;
        }

        visited.add(visit);

        Set<String> toVisit = graph.get(visit);

        if (toVisit != null) {

            LinkedList<String> intersection = new LinkedList<>(toVisit);
            intersection.retainAll(visited);
            if (intersection.size() != 0) {
                String firstLink = intersection.getFirst();
                return new LinkedList<>(Collections.singletonList(firstLink));
            }

            for (String nextVisit : toVisit) {
                LinkedList<String> cycle = cycleWarning_Helper(nextVisit, visited, graph);
                if (cycle != null) {
                    cycle.push(nextVisit);
                    return cycle;
                }
            }
        }
        visited.remove(visit);

        return null;
    }

    public static boolean containsTransitiveAgreeAnnex(ComponentInstance compInst, boolean isMonolithic) {
    	Subcomponent subComp = compInst.getSubcomponent();
    	if(!isMonolithic){
    		return typeContainsAgreeAnnex(subComp);
    	}
    	
    	if (containsAgreeAnnex(subComp)) {
            return true;
        }
        EList<ComponentInstance> transitiveSubs = compInst.getAllComponentInstances();
        for (ComponentInstance transInst : transitiveSubs) {
            if (AgreeUtils.containsAgreeAnnex(transInst.getSubcomponent())) {
                return true;
            }
        }
        return false;
    }

    
    public static boolean typeContainsAgreeAnnex(Subcomponent subComp) {
        ComponentType compType = subComp.getComponentType();
        if (compType != null) {
            if (AnnexUtil.getAllAnnexSubclauses(compType, AgreePackage.eINSTANCE.getAgreeContractSubclause())
                    .size() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean containsAgreeAnnex(Subcomponent subComp) {
        ComponentImplementation compImpl = subComp.getComponentImplementation();
        if (compImpl != null) {
            if (AnnexUtil.getAllAnnexSubclauses(compImpl, AgreePackage.eINSTANCE.getAgreeContractSubclause())
                    .size() > 0) {
                return true;
            }
        }

        ComponentType compType = subComp.getComponentType();
        if (compType != null) {
            if (AnnexUtil.getAllAnnexSubclauses(compType, AgreePackage.eINSTANCE.getAgreeContractSubclause())
                    .size() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public static Expr getInitValueFromType(Type type){
        if(type instanceof NamedType){
            return getInitValueFromType((NamedType)type);
        }
        if(type instanceof RecordType){
            RecordType recordType = (RecordType)type;
            Map<String, Expr> fieldMap = new HashMap<>();
            for(Entry<String, Type> entry : recordType.fields.entrySet()){
                Expr subExpr = getInitValueFromType(entry.getValue());
                fieldMap.put(entry.getKey(), subExpr);
            }
            return new RecordExpr(recordType.id, fieldMap);
        }
        throw new AgreeException("AGREE cannot figure out an initial type for Lustre type: "+type.getClass());
    }
    
    private static Expr getInitValueFromType(NamedType type){
        if(typeMatchesBool(type)){
            return new BoolExpr(false);
        }
        if(typeMatchesInteger(type)){
            return new IntExpr(BigInteger.ZERO);
        }
        if(typeMatchesReal(type)){
            return new RealExpr(BigDecimal.ZERO);
        }
        throw new AgreeException("Unhandled initial type for type '"+type+"'");
    }

    public static boolean referenceIsInContract(EObject reference, ComponentInstance compInst) {
    	ComponentClassifier compClass = compInst.getComponentClassifier();
    	if(compClass instanceof ComponentImplementation){
    		compClass = ((ComponentImplementation)compClass).getType();
    	}
        if(reference == null){
            return false;
        }
        EObject container = reference;
        while (!(container instanceof ComponentClassifier)) {
            container = container.eContainer();
        }
        if (container instanceof ComponentImplementation) {
            return false;
        }
        return container == compClass;
    }
    
    public static GlobalURIEditorOpener getGlobalURIEditorOpener() {
        if (globalURIEditorOpener == null) {
            AgreeActivator activator = AgreeActivator.getInstance();
            String language = AgreeActivator.COM_ROCKWELLCOLLINS_ATC_AGREE_AGREE;
            Injector injector = activator.getInjector(language);
            globalURIEditorOpener = injector.getInstance(GlobalURIEditorOpener.class);
        }
        return globalURIEditorOpener;
    }
    
    public static ComponentImplementation compImplFromType(ComponentType ct) {
        ComponentImplementation ci;
        if (ct instanceof ThreadType) {
            ci = Aadl2Factory.eINSTANCE.createThreadImplementation();
        } else if (ct instanceof ThreadGroupType) {
            ci = Aadl2Factory.eINSTANCE.createThreadGroupImplementation();
        } else if (ct instanceof ProcessType) {
            ci = Aadl2Factory.eINSTANCE.createProcessImplementation();
        } else if (ct instanceof SubprogramType) {
            ci = Aadl2Factory.eINSTANCE.createSubprogramImplementation();
        } else if (ct instanceof ProcessorType) {
            ci = Aadl2Factory.eINSTANCE.createProcessorImplementation();
        } else if (ct instanceof BusType) {
            ci = Aadl2Factory.eINSTANCE.createBusImplementation();
        } else if (ct instanceof DeviceType) {
            ci = Aadl2Factory.eINSTANCE.createDeviceImplementation();
        } else if (ct instanceof SystemType){
            ci = Aadl2Factory.eINSTANCE.createSystemImplementation();
        } else {
            throw new AgreeException("Unhandled component type: "+ct.getClass().toString());
        }
        ci.setName(ct.getName() + ".wrapper");
        ci.setType(ct);
        ct.eResource().getContents().add(ci);

        return ci;
    }
    
    public static boolean typeMatchesBool(NamedType type){
        return type.equals(NamedType.BOOL) ||
                type.toString().startsWith("Base_Types__Boolean");
    }
    
    public static boolean typeMatchesInteger(NamedType type){
        return type.equals(NamedType.INT) ||
                type.toString().startsWith("Base_Types__Unsigned") ||
                type.toString().startsWith("Base_Types__Integer");
    }
    
    public static boolean typeMatchesReal(NamedType type){
        return type.equals(NamedType.REAL) ||
                type.toString().startsWith("Base_Types__Float");
    }
    
    public static List<TypeDef> getLustreTypes(AgreeProgram agreeProgram) {
        List<TypeDef> types = new ArrayList<>();
		for (Type type : agreeProgram.globalTypes) {
			String typeName;
			if (type instanceof RecordType) {
				typeName = ((RecordType) type).id;
			} else if (type instanceof EnumType) {
				typeName = ((EnumType) type).id;
			}else{
				throw new AgreeException("Unable to handle type of type '"+type.getClass()+"'");
			}
            types.add(new TypeDef(typeName, type));
        }
        
        //add synonym types
        types.addAll(getTypeSynonmyms());
        return types;
    }
    
    private static Collection<? extends TypeDef> getTypeSynonmyms() {
        List<TypeDef> types = new ArrayList<>();
        
        types.add(new TypeDef("Base_Types__Boolean", NamedType.BOOL));
        types.add(new TypeDef("Base_Types__Unsigned", NamedType.INT));
        types.add(new TypeDef("Base_Types__Unsigned_64", NamedType.INT));
        types.add(new TypeDef("Base_Types__Unsigned_32", NamedType.INT));
        types.add(new TypeDef("Base_Types__Unsigned_16", NamedType.INT));
        types.add(new TypeDef("Base_Types__Unsigned_8", NamedType.INT));
        types.add(new TypeDef("Base_Types__Integer", NamedType.INT));
        types.add(new TypeDef("Base_Types__Integer_64", NamedType.INT));
        types.add(new TypeDef("Base_Types__Integer_32", NamedType.INT));
        types.add(new TypeDef("Base_Types__Integer_16", NamedType.INT));
        types.add(new TypeDef("Base_Types__Integer_8", NamedType.INT));
        types.add(new TypeDef("Base_Types__Float", NamedType.REAL));
        types.add(new TypeDef("Base_Types__Float_32", NamedType.REAL));
        types.add(new TypeDef("Base_Types__Float_64", NamedType.REAL));
        
        return types;
    }

}
