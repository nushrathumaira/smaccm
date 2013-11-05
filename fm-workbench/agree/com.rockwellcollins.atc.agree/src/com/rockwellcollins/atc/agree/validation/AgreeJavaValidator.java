/*
 * generated by Xtext
 */
package com.rockwellcollins.atc.agree.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Check;
import org.osate.aadl2.AadlBoolean;
import org.osate.aadl2.AadlInteger;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.AadlReal;
import org.osate.aadl2.AadlString;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.ClassifierFeature;
import org.osate.aadl2.ClassifierType;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DataSubcomponentType;
import org.osate.aadl2.DataType;
import org.osate.aadl2.Element;
import org.osate.aadl2.EnumerationType;
import org.osate.aadl2.Feature;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Namespace;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyType;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.ThreadSubcomponent;
import org.osate.aadl2.impl.SubcomponentImpl;

import com.rockwellcollins.atc.agree.agree.AgreeContract;
import com.rockwellcollins.atc.agree.agree.AgreePackage;
import com.rockwellcollins.atc.agree.agree.AgreeSubclause;
import com.rockwellcollins.atc.agree.agree.Arg;
import com.rockwellcollins.atc.agree.agree.AssertStatement;
import com.rockwellcollins.atc.agree.agree.AssumeStatement;
import com.rockwellcollins.atc.agree.agree.BinaryExpr;
import com.rockwellcollins.atc.agree.agree.BoolLitExpr;
import com.rockwellcollins.atc.agree.agree.CallDef;
import com.rockwellcollins.atc.agree.agree.ConstStatement;
import com.rockwellcollins.atc.agree.agree.EqStatement;
import com.rockwellcollins.atc.agree.agree.Expr;
import com.rockwellcollins.atc.agree.agree.FnCallExpr;
import com.rockwellcollins.atc.agree.agree.FnDefExpr;
import com.rockwellcollins.atc.agree.agree.GetPropertyExpr;
import com.rockwellcollins.atc.agree.agree.GuaranteeStatement;
import com.rockwellcollins.atc.agree.agree.IdExpr;
import com.rockwellcollins.atc.agree.agree.IfThenElseExpr;
import com.rockwellcollins.atc.agree.agree.IntLitExpr;
import com.rockwellcollins.atc.agree.agree.LemmaStatement;
import com.rockwellcollins.atc.agree.agree.LiftStatement;
import com.rockwellcollins.atc.agree.agree.NestedDotID;
import com.rockwellcollins.atc.agree.agree.NextExpr;
import com.rockwellcollins.atc.agree.agree.NodeDefExpr;
import com.rockwellcollins.atc.agree.agree.NodeEq;
import com.rockwellcollins.atc.agree.agree.NodeLemma;
import com.rockwellcollins.atc.agree.agree.NodeStmt;
import com.rockwellcollins.atc.agree.agree.PreExpr;
import com.rockwellcollins.atc.agree.agree.PrevExpr;
import com.rockwellcollins.atc.agree.agree.PropertyStatement;
import com.rockwellcollins.atc.agree.agree.RealLitExpr;
import com.rockwellcollins.atc.agree.agree.ThisExpr;
import com.rockwellcollins.atc.agree.agree.UnaryExpr;

/**
 * Custom validation rules.
 * 
 * see http://www.eclipse.org/Xtext/documentation.html#validation
 */
public class AgreeJavaValidator extends
        com.rockwellcollins.atc.agree.validation.AbstractAgreeJavaValidator {

    private final static AgreeType BOOL = new AgreeType("bool");
    private final static AgreeType INT = new AgreeType("int");
    private final static AgreeType REAL = new AgreeType("real");
    private final static AgreeType ERROR = new AgreeType("<error>");

    @Override
    protected boolean isResponsible(Map<Object, Object> context, EObject eObject) {
        return (eObject.eClass().getEPackage() == AgreePackage.eINSTANCE);
    }

    public AgreeType getAgreeType(Arg arg) {
        return new AgreeType(arg.getType().getString());
    }

    @Check
    public void checkAssume(AssumeStatement assume) {
        AgreeType exprType = getAgreeType(assume.getExpr());
        if (!matches(BOOL, exprType)) {
            error(assume, "Expression for assume statement is of type '" + exprType.toString()
                    + "' but must be of type 'bool'");
        }
    }
    
    @Check
    public void checkLift(LiftStatement lift){
        NestedDotID dotId = lift.getSubcomp();
        
        if(dotId.getSub() != null){
            error(lift, "Lift statements can only be applied to direct subcomponents." +
                        "Place a lift statement in the subcomponents contract for heavy lifting");
        }
        
        NamedElement namedEl = dotId.getBase();
        

        if(namedEl != null){
            if(!(namedEl instanceof SubcomponentImpl)){
                error(lift, "Lift statements must apply to subcomponent implementations. '"
                        +namedEl.getName()+"' is not a subcomponent.");
            }else{
                SubcomponentImpl subImpl = (SubcomponentImpl)namedEl;
                if(subImpl.getComponentImplementation() == null){
                    error(lift, "Lift statements must apply to subcomponent implementations. '"
                            +namedEl.getName()+"' is a subcomponent type, not a subcomponent implementation.");
                }
            }
        }
    }

    @Check
    public void checkAssert(AssertStatement asser) {
        Classifier comp = asser.getContainingClassifier();
        if (!(comp instanceof ComponentImplementation)) {
            error(asser, "Assert statements are only allowed in component implementations.");
        }

        AgreeType exprType = getAgreeType(asser.getExpr());
        if (!matches(BOOL, exprType)) {
            error(asser, "Expression for assert statement is of type '" + exprType.toString()
                    + "' but must be of type 'bool'");
        }

    }

    @Check
    public void checkGuarantee(GuaranteeStatement guar) {
        Classifier comp = guar.getContainingClassifier();
        if (!(comp instanceof ComponentType)) {
            error(guar, "Guarantee statements are only allowed in component types");
        }

        AgreeType exprType = getAgreeType(guar.getExpr());
        if (!matches(BOOL, exprType)) {
            error(guar, "Expression for guarantee statement is of type '" + exprType.toString()
                    + "' but must be of type 'bool'");
        }
    }

    @Check
    public void checkLemma(LemmaStatement lemma) {
        Classifier comp = lemma.getContainingClassifier();
        if (!(comp instanceof ComponentImplementation)) {
            error(lemma, "Lemma statements are only allowed in component implementations and nodes");
        }

        AgreeType exprType = getAgreeType(lemma.getExpr());
        if (!matches(BOOL, exprType)) {
            error(lemma, "Expression for lemma statement is of type '" + exprType.toString()
                    + "' but must be of type 'bool'");
        }
    }

    @Check
    public void checkUnaryExpr(UnaryExpr unaryExpr) {
        AgreeType typeRight = getAgreeType(unaryExpr.getExpr());
        String op = unaryExpr.getOp();

        switch (op) {
        case "-":
            if (!matches(INT, typeRight) && !matches(REAL, typeRight)) {
                error(unaryExpr, "right side of unary expression '" + op + "' is of type '"
                        + typeRight + "' but must be of type 'int' or 'real'");
            }
            break;
        case "not":
            if (!matches(BOOL, typeRight)) {
                error(unaryExpr, "right side of unary expression '" + op + "' is of type '"
                        + typeRight + "' but must be of type 'bool'");
            }
            break;
        default:
            assert (false);
        }
    }

    private AgreeType getAgreeType(UnaryExpr unaryExpr) {
        return getAgreeType(unaryExpr.getExpr());
    }

    private AgreeType getAgreeType(NextExpr nextExpr) {
        return getAgreeType(nextExpr.getExpr());
    }

    private AgreeType getAgreeType(NestedDotID nestDotIdExpr) {
        return getAgreeType(getFinalNestId(nestDotIdExpr));
    }

    private AgreeType getAgreeType(NamedElement namedEl) {
        if (namedEl instanceof Property) {
            Property propVal = (Property) namedEl;
            PropertyType propType = propVal.getPropertyType();

            if (propType instanceof AadlBoolean) {
                return BOOL;
            } else if (propType instanceof AadlString || propType instanceof EnumerationType) {
                return new AgreeType("string");
            } else if (propType instanceof AadlInteger) {
                return INT;
            } else if (propType instanceof AadlReal) {
                return REAL;
            } else if (propType instanceof ClassifierType) {
                return new AgreeType("component");
            }
        } else if (namedEl instanceof DataSubcomponent) {
            // this is for checking "Base_Types::Boolean" etc...
            return getAgreeType((DataSubcomponent) namedEl);
        } else if (namedEl instanceof Arg) {
            return getAgreeType((Arg) namedEl);
        } else if (namedEl instanceof ClassifierType || namedEl instanceof Subcomponent) {
            return new AgreeType("component");
        } else if (namedEl instanceof PropertyStatement) {
            return getAgreeType((PropertyStatement) namedEl);
        } else if (namedEl instanceof ConstStatement) {
            return getAgreeType((ConstStatement) namedEl);
        } else if (namedEl instanceof EqStatement) {
            return getAgreeType(namedEl);
        } else if (namedEl instanceof DataPort){
            return getAgreeType(((DataPort)namedEl).getDataFeatureClassifier());
        }

        return ERROR;
    }

    private AgreeType getAgreeType(DataSubcomponent data) {
        ComponentClassifier dataClass = data.getAllClassifier();

        while (dataClass != null) {
            switch (dataClass.getQualifiedName()) {
            case "Base_Types::Boolean":
                return BOOL;
            case "Base_Types::Integer":
                return INT;
            case "Base_Types::Float":
                return REAL;
            }

            DataType dataType = (DataType) dataClass;
            dataClass = dataType.getExtended();
        }

        return new AgreeType("uninterpreted data");
    }
    
    private AgreeType getAgreeType(DataSubcomponentType data) {
        String qualName = data.getQualifiedName();
        switch (qualName) {
        case "Base_Types::Boolean":
            return BOOL;
        case "Base_Types::Integer":
            return INT;
        case "Base_Types::Float":
            return REAL;
        }
        return new AgreeType("uninterpreted data");
    }

    @Check
    public void checkPropertyStatement(PropertyStatement propStat) {
        AgreeType exprType = getAgreeType(propStat.getExpr());
        if (!matches(BOOL, exprType)) {
            error(propStat, "Property statement '" + propStat.getName() + "' is of type '"
                    + exprType + "' but must be of type 'bool'");
        }

    }

    private AgreeType getAgreeType(PropertyStatement propStat) {
        return getAgreeType(propStat.getExpr());
    }

    @Check
    public void checkConstStatement(ConstStatement constStat) {
        AgreeType expected = new AgreeType(constStat.getType().getString());
        AgreeType actual = getAgreeType(constStat.getExpr());

        if (!matches(expected, actual)) {
            error(constStat, "The assumed type of constant statement '" + constStat.getName()
                    + "' is '" + expected + "' but the actual type is '" + actual + "'");
        }

    }
    

    @Check
    public void checkNamedElement(NamedElement namedEl){
      
        //check for namespace collision in component types of component implementations
        //and for collisions between subcomponent and feature names
        
        EObject container = namedEl.eContainer();
        while(!(container instanceof AadlPackage
                || container instanceof ComponentImplementation
                || container instanceof ComponentType)){
            container = container.eContainer();
        }
        
        ComponentImplementation compImpl = null;
        ComponentType type = null;
        if(container instanceof ComponentImplementation){
            compImpl = (ComponentImplementation)container;
            type = compImpl.getType();
            checkDupNames(namedEl, type, compImpl);
        }else{
            if(container instanceof ComponentType){
                type = (ComponentType) container;
            }
        }
        
        if(type != null){
            for(Feature feat : type.getAllFeatures()){
                if(namedEl.getName().equals(feat.getName())){
                    error(feat, "Element of the same name ('"+namedEl.getName()
                            +"') in AGREE Annex in '"
                            +(compImpl == null ? type.getName() : compImpl.getName()) +"'");
                    error(namedEl, "Feature of the same name ('"+namedEl.getName()
                            +"') in component type");
                }
            }
        }
        
    }

    private void checkDupNames(NamedElement namedEl, ComponentType type,
            ComponentImplementation compImpl) {
        NamedElement match = matchedInAgreeAnnex(type, namedEl.getName());
        
        if(match != null){
            error(match, "Element of the same name ('"+namedEl.getName()
                    +"') in component implementation '"
                    +compImpl.getName()+"'");
            error(namedEl, "Element of the same name ('"+namedEl.getName()
                    +"') in component type");
        }
        
        for(Subcomponent sub : compImpl.getAllSubcomponents()){
            if(namedEl.getName().equals(sub.getName())){
                error(sub, "Element of the same name ('"+namedEl.getName()
                        +"') in AGREE Annex in '"
                        +compImpl.getName()+"'");
                error(namedEl, "Subcomponent of the same name ('"+namedEl.getName()
                        +"') in component implementation");
            }
        }
    }
    
   
    private NamedElement matchedInAgreeAnnex(ComponentClassifier compClass, String name){
        
        for(AnnexSubclause subClause : compClass.getAllAnnexSubclauses()){
            if(subClause instanceof AgreeSubclause){
                AgreeContract contr = (AgreeContract) subClause.getChildren().get(0);
                for(EObject obj : contr.getChildren()){
                    if(obj instanceof NamedElement){
                        if(name.equals(((NamedElement)obj).getName())){
                            return (NamedElement) obj;
                        }
                    }
                }
            }
        }
        return null;
    }

    private AgreeType getAgreeType(ConstStatement constStat) {
        return new AgreeType(constStat.getType().getString());
    }

    private void checkMultiAssignEq(EObject src, List<Arg> lhsArgs, Expr rhsExpr) {
        List<AgreeType> agreeLhsTypes = typesFromArgs(lhsArgs);
        List<AgreeType> agreeRhsTypes = new ArrayList<>();

        if (rhsExpr instanceof FnCallExpr) {
            NamedElement namedEl = getFinalNestId(((FnCallExpr) rhsExpr).getFn());
            if (namedEl instanceof NodeDefExpr) {
                NodeDefExpr nodeDef = (NodeDefExpr) namedEl;
                for (Arg var : nodeDef.getRets()) {
                    agreeRhsTypes.add(new AgreeType(var.getType().getString()));
                }
            } else {
                assert (namedEl instanceof FnDefExpr);
                FnDefExpr fnDef = (FnDefExpr) namedEl;
                agreeRhsTypes.add(new AgreeType(fnDef.getType().getString()));
            }
        } else {
            agreeRhsTypes.add(getAgreeType(rhsExpr));
        }

        if (agreeLhsTypes.size() != agreeRhsTypes.size()) {
            error(src, "Equation assigns " + agreeLhsTypes.size()
                    + " variables, but right side returns " + agreeRhsTypes.size() + " values");
            return;
        }

        for (int i = 0; i < agreeLhsTypes.size(); i++) {
            AgreeType lhsType = agreeLhsTypes.get(i);
            AgreeType rhsType = agreeRhsTypes.get(i);

            if (!matches(rhsType, lhsType)) {
                error(src, "The variable '" + lhsArgs.get(i).getName()
                        + "' on the left side of equation is of type '" + lhsType
                        + "' but must be of type '" + rhsType + "'");
            }
        }
    }

    @Check
    public void checkEqStatement(EqStatement eqStat) {
        checkMultiAssignEq(eqStat, eqStat.getLhs(), eqStat.getExpr());
    }

    @Check
    public void checkNodeEq(NodeEq nodeEq) {
        checkMultiAssignEq(nodeEq, nodeEq.getLhs(), nodeEq.getExpr());
    }

    @Check
    public void checkNodeLemma(NodeLemma nodeLemma) {
        AgreeType exprType = getAgreeType(nodeLemma.getExpr());
        if (!matches(BOOL, exprType)) {
            error(nodeLemma, "Expression for lemma statement is of type '" + exprType
                    + "' but must be of type 'bool'");
        }
    }

    @Check
    public void checkNodeDef(NodeDefExpr nodeDefExpr) {
        
        if(nodeDefExpr.getNodeBody() == null){
            return; //this will throw a parse error anyway
        }
        
        Map<Arg, Integer> assignMap = new HashMap<>();
        for (Arg arg : nodeDefExpr.getRets()) {
            assignMap.put(arg, 0);
        }
        for (Arg arg : nodeDefExpr.getNodeBody().getLocs()) {
            assignMap.put(arg, 0);
        }

        for (NodeStmt stmt : nodeDefExpr.getNodeBody().getStmts()) {
            if (stmt instanceof NodeEq) {
                NodeEq eq = (NodeEq) stmt;
                for (Arg arg : eq.getLhs()) {
                    Integer value = assignMap.get(arg);
                    if (value == null) {
                        error("Equation attempting to assign '" + arg.getName()
                                + "', which is not an assignable value within the node");
                        return;
                    } else {
                        assignMap.put(arg, value + 1);
                    }
                }
            }
        }
        for (Map.Entry<Arg, Integer> elem : assignMap.entrySet()) {
            if (elem.getValue() == 0) {
                error("Variable '" + elem.getKey().getName()
                        + "' is never assigned by an equation in node '" + nodeDefExpr.getName()
                        + "'");
                return;
            } else if (elem.getValue() > 1) {
                error("Variable '" + elem.getKey().getName()
                        + "' is assigned multiple times in node '" + nodeDefExpr.getName() + "'");
            }
        }
    }

    @Check
    public void checkThisExpr(ThisExpr thisExpr){
        //these should only appear in Get_Property expressions
        
        if(!(thisExpr.eContainer() instanceof GetPropertyExpr)){
            error(thisExpr, "'this' expressions can only be used in 'Get_Property' expressions.");
        }
        
    }
    
    @Check
    public void checkGetPropertyExpr(GetPropertyExpr getPropExpr) {
        AgreeType compType = getAgreeType(getPropExpr.getComponent());
        // AgreeType propType = getAgreeType(propExpr.getName());
        Expr propExpr = getPropExpr.getProp();

        if (!compType.equals("component")) {
            error(getPropExpr, "the first argument of the 'Get_Property' function"
                    + " is of type '" + compType.toString() + "' but must be of some"
                    + " aadl component type.");
        }

        if (!(propExpr instanceof IdExpr)) {
            error(getPropExpr, "the second argument of the 'Get_Property' function"
                    + "must be some aadl property");
            return;
        }

        NamedElement idVal = ((IdExpr) propExpr).getId();
        if (!(idVal instanceof Property)) {
            error(getPropExpr, "the second argument of the 'Get_Property' function"
                    + "must be some aadl property");
        }
    }

    private AgreeType getAgreeType(GetPropertyExpr getPropExpr) {
        return getAgreeType(getPropExpr.getProp());
    }

    @Check
    public void checkPrevExpr(PrevExpr prevExpr) {
        AgreeType delayType = getAgreeType(prevExpr.getDelay());
        AgreeType initType = getAgreeType(prevExpr.getInit());

        if (!matches(initType, delayType)) {
            error(prevExpr,
                    "The first and second arguments of the 'prev' function are of non-matching types '"
                            + delayType + "' and '" + initType + "'");
        }
    }

    private AgreeType getAgreeType(PrevExpr prevExpr) {
        return getAgreeType(prevExpr.getDelay());
    }

    private List<AgreeType> getAgreeTypes(List<? extends Expr> exprs) {
        ArrayList<AgreeType> list = new ArrayList<>();
        for (Expr expr : exprs) {
            list.add(getAgreeType(expr));
        }
        return list;
    }

    public List<AgreeType> typesFromArgs(List<Arg> args) {
        ArrayList<AgreeType> list = new ArrayList<>();
        for (Arg arg : args) {
            list.add(getAgreeType(arg));
        }
        return list;
    }

    public void checkInputsVsActuals(FnCallExpr fnCall) {
        NestedDotID dotId = fnCall.getFn();
        NamedElement namedEl = getFinalNestId(dotId);

        if (!(namedEl instanceof CallDef)) {
            // this error will be caught elsewhere
            return;
        }

        CallDef callDef = (CallDef) namedEl;

        List<AgreeType> inDefTypes;
        String callName;

        // extract in/out arguments
        if (callDef instanceof FnDefExpr) {
            FnDefExpr fnDef = (FnDefExpr) callDef;
            inDefTypes = typesFromArgs(fnDef.getArgs());
            callName = fnDef.getName();
        } else if (callDef instanceof NodeDefExpr) {
            NodeDefExpr nodeDef = (NodeDefExpr) callDef;
            inDefTypes = typesFromArgs(nodeDef.getArgs());
            callName = nodeDef.getName();
        } else {
            error(fnCall, "Node or Function definition name expected.");
            return;
        }

        // extract args
        List<AgreeType> argCallTypes = getAgreeTypes(fnCall.getArgs());

        if (inDefTypes.size() != argCallTypes.size()) {
            error(fnCall, "Function definition '" + callName + "' requires " + inDefTypes.size()
                    + " arguments, but this function call provides " + argCallTypes.size()
                    + " arguments");
            return;
        }

        for (int i = 0; i < inDefTypes.size(); i++) {
            AgreeType callType = argCallTypes.get(i);
            AgreeType defType = inDefTypes.get(i);

            if (!matches(defType, callType)) {
                error(fnCall, "Argument " + i + " of function call '" + callName + "' is of type '"
                        + callType + "' but must be of type '" + defType + "'");
            }
        }
    }

    @Check
    public void checkFnCallExpr(FnCallExpr fnCall) {
        checkInputsVsActuals(fnCall);
    }

    private AgreeType getAgreeType(FnCallExpr fnCall) {
        // TODO: Examine type system in more detail
        // TODO: Fix to make support type lists.

        NestedDotID dotId = fnCall.getFn();
        NamedElement namedEl = getFinalNestId(dotId);

        // extract in/out arguments
        if (namedEl instanceof FnDefExpr) {
            FnDefExpr fnDef = (FnDefExpr) namedEl;
            return new AgreeType(fnDef.getType().getString());
        } else if (namedEl instanceof NodeDefExpr) {
            NodeDefExpr nodeDef = (NodeDefExpr) namedEl;
            List<AgreeType> outDefTypes = typesFromArgs(nodeDef.getRets());
            if (outDefTypes.size() == 1) {
                return outDefTypes.get(0);
            } else {
                error(fnCall, "Nodes embedded in expressions must have exactly one return value."
                        + "  Node " + nodeDef.getName() + " contains " + outDefTypes.size()
                        + " return values");
                return null;
            }
        } else {
            error(fnCall, "Node or Function definition name expected.");
            return null;
        }
    }

    @Check
    public void checkIfThenElseExpr(IfThenElseExpr expr) {
        AgreeType condType = getAgreeType(expr.getA());
        AgreeType thenType = getAgreeType(expr.getB());
        AgreeType elseType = getAgreeType(expr.getC());

        if (!matches(BOOL, condType)) {
            error(expr, "The condition of the if statement is of type '" + condType
                    + "' but must be of type 'bool'");
        }

        if (!matches(elseType, thenType)) {
            error(expr, "The 'then' and 'else' expressions are of non-matching types '" + thenType
                    + "' and '" + elseType + "'");
        }
    }

    private AgreeType getAgreeType(IfThenElseExpr expr) {
        return getAgreeType(expr.getB());
    }

    @Check
    public void checkBinaryExpr(BinaryExpr binExpr) {
        AgreeType typeLeft = getAgreeType(binExpr.getLeft());
        AgreeType typeRight = getAgreeType(binExpr.getRight());
        String op = binExpr.getOp();

        switch (op) {
        case "->":
            if (!matches(typeRight, typeLeft)) {
                error(binExpr, "left and right sides of binary expression '" + op
                        + "' are of type '" + typeLeft + "' and '" + typeRight
                        + "', but must be of the same type");
            }
            return;

        case "=>":
        case "<=>":
        case "and":
        case "or":
            if (!matches(BOOL, typeLeft)) {
                error(binExpr, "left side of binary expression '" + op + "' is of type '"
                        + typeLeft.toString() + "' but must be of " + "type 'bool'");
            }
            if (!matches(BOOL, typeRight)) {
                error(binExpr, "right side of binary expression '" + op + "' is of type '"
                        + typeRight.toString() + "' but must be of" + " type 'bool'");
            }
            return;

        case "<>":
        case "!=":
            if (!matches(typeRight, typeLeft)) {
                error(binExpr, "left and right sides of binary expression '" + op
                        + "' are of type '" + typeLeft + "' and '" + typeRight
                        + "', but must be of the same type");
            }
            return;

        case "<":
        case "<=":
        case ">":
        case ">=":
        case "=":
        case "+":
        case "-":
        case "*":
        case "/":
        case "mod":
        case "div":
            if (!matches(typeRight, typeLeft)) {
                error(binExpr, "left and right sides of binary expression '" + op
                        + "' are of type '" + typeLeft + "' and '" + typeRight
                        + "', but must be of the same type");
            }
            if (!matches(INT, typeLeft) && !matches(REAL, typeLeft)) {
                error(binExpr, "left side of binary expression '" + op + "' is of type '"
                        + typeLeft + "' but must be of type" + "'int' or 'real'");
            }
            if (!matches(INT, typeRight) && !matches(REAL, typeRight)) {
                error(binExpr, "right side of binary expression '" + op + "' is of type '"
                        + typeRight + "' but must be of type" + "'int' or 'real'");
            }
            return;

        default:
            assert (false);
        }
    }

    private AgreeType getAgreeType(BinaryExpr binExpr) {
        AgreeType typeLeft = getAgreeType(binExpr.getLeft());
        String op = binExpr.getOp();

        switch (op) {
        case "->":
            return typeLeft;
        case "=>":
        case "<=>":
        case "and":
        case "or":
            return BOOL;
        case "<>":
        case "!=":
            return BOOL;
        case "<":
        case "<=":
        case ">":
        case ">=":
        case "=":
            return BOOL;
        case "+":
        case "-":
        case "*":
        case "/":
        case "mod":
        case "div":
            return typeLeft;
        }

        return ERROR;
    }

    private Boolean hasCallDefParent(Element e) {
        while (e != null) {
            if (e instanceof CallDef) {
                return true;
            }
            e = e.getOwner();
        }
        return false;
    }

    // TODO: Don't we need more validation here? What if the Id of the IdExpr

    private void checkScope(Expr expr, NamedElement id) {
        if (hasCallDefParent(expr)) {
            if (!hasCallDefParent(id) && !(id instanceof ConstStatement)) {
                error("Unknown identifier Id: '"
                        + id
                        + "' (Note that nodes can only refer to inputs, outputs, and local variables and global constants).");
            }
        }
    }

    @Check
    public void checkIdExpr(IdExpr idExpr) {
        // Scope check for nodes / functions
        NamedElement id = idExpr.getId();
        checkScope(idExpr, id);
        
        if(id instanceof Property){
            if(!(idExpr.eContainer() instanceof GetPropertyExpr)){
                error(idExpr, "References to AADL properties can only appear in 'Get_Property' expressions.");
            }
        }
    }

    private AgreeType getAgreeType(IdExpr idExpr) {

        NamedElement id = idExpr.getId();
        return getAgreeType(id);
    }

    private AgreeType getAgreeType(Expr expr) {
        if (expr instanceof BinaryExpr) {
            return getAgreeType((BinaryExpr) expr);
        } else if (expr instanceof FnCallExpr) {
            return getAgreeType((FnCallExpr) expr);
        } else if (expr instanceof IfThenElseExpr) {
            return getAgreeType((IfThenElseExpr) expr);
        } else if (expr instanceof PrevExpr) {
            return getAgreeType((PrevExpr) expr);
        } else if (expr instanceof NextExpr) {
            return getAgreeType((NextExpr) expr);
        } else if (expr instanceof GetPropertyExpr) {
            return getAgreeType((GetPropertyExpr) expr);
        } else if (expr instanceof NestedDotID) {
            return getAgreeType((NestedDotID) expr);
        } else if (expr instanceof UnaryExpr) {
            return getAgreeType((UnaryExpr) expr);
        } else if (expr instanceof IdExpr) {
            return getAgreeType((IdExpr) expr);
        } else if (expr instanceof IntLitExpr) {
            return INT;
        } else if (expr instanceof RealLitExpr) {
            return REAL;
        } else if (expr instanceof BoolLitExpr) {
            return BOOL;
        } else if (expr instanceof ThisExpr) {
            return new AgreeType("component");
        } else if (expr instanceof PreExpr) {
            return getAgreeType(((PreExpr) expr).getExpr());
        }

        return ERROR;
    }

    public NamedElement getFinalNestId(NestedDotID dotId) {
        while (dotId.getSub() != null) {
            dotId = dotId.getSub();
        }
        return dotId.getBase();
    }

    public static boolean matches(AgreeType expected, AgreeType actual) {
        if (expected == null || actual == null) {
            return true;
        }

        return expected.equals(actual);
    }
}
