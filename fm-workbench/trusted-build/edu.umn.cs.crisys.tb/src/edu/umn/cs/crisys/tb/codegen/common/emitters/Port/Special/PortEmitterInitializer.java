package edu.umn.cs.crisys.tb.codegen.common.emitters.Port.Special;
import java.io.File;
import java.util.List;
import java.util.Map;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import edu.umn.cs.crisys.tb.TbException;
import edu.umn.cs.crisys.tb.codegen.common.emitters.EmitterFactory;
import edu.umn.cs.crisys.tb.codegen.common.emitters.Port.PortEmitterCamkes;
import edu.umn.cs.crisys.tb.codegen.common.emitters.Port.PortEmitterEChronos;
import edu.umn.cs.crisys.tb.codegen.common.emitters.Port.PortEmitterVxWorks;
import edu.umn.cs.crisys.tb.codegen.common.names.ModelNames;
import edu.umn.cs.crisys.tb.codegen.common.names.ThreadImplementationNames;
import edu.umn.cs.crisys.tb.codegen.common.names.TypeNames;
import edu.umn.cs.crisys.tb.model.OSModel;
import edu.umn.cs.crisys.tb.model.connection.PortConnection;
import edu.umn.cs.crisys.tb.model.port.DispatchableInputPort;
import edu.umn.cs.crisys.tb.model.port.ExternalHandler;
import edu.umn.cs.crisys.tb.model.port.InitializerPort;
import edu.umn.cs.crisys.tb.model.port.InputDataPort;
import edu.umn.cs.crisys.tb.model.port.InputEventPort;
import edu.umn.cs.crisys.tb.model.port.InputPort;
import edu.umn.cs.crisys.tb.model.port.OutputDataPort;
import edu.umn.cs.crisys.tb.model.port.OutputEventPort;
import edu.umn.cs.crisys.tb.model.port.OutputPort;
import edu.umn.cs.crisys.tb.model.port.PortFeature;
import edu.umn.cs.crisys.tb.model.type.BoolType;
import edu.umn.cs.crisys.tb.model.type.IntType;
import edu.umn.cs.crisys.tb.model.type.Type;
import edu.umn.cs.crisys.tb.model.type.UnitType;
import edu.umn.cs.crisys.tb.util.Util;

public class PortEmitterInitializer implements PortEmitterCamkes, PortEmitterEChronos, PortEmitterVxWorks {

   public static boolean isApplicable(PortFeature pf) {
      // right kind of port
      boolean ok = (pf instanceof InitializerPort);
      return ok;
   }
   
   private PortFeature port;
   private OSModel model; 
   Type indexType = new IntType(32, false); 
   
   public PortEmitterInitializer(PortFeature pf) {
      this.port = pf;
      this.model = Util.getElementOSModel(pf);
   }
   
   @Override
   public void addPortPublicTypeDeclarations(Map<String, Type> typeList) {
      // no-op for the moment; I could add support for 
      // auto-declaration of types, but right now this is already 
      // handled.
   }

   @Override
   public PortFeature getModelElement() {
      return this.port;
   }

   @Override
   public void getWritePortCFiles(File directory) {
      // no-op for InitializerPorts
   }

   @Override
   public void getWritePortHFiles(File directory) {
      // no-op for InitializerPorts
   }
   
   @Override
   public String getWritePortHPrototypes() {
      // no-op for InitializerPorts
      return ""; 
   }


   @Override
   public String getWritePortDeclarations() {
      // no-op for InitializerPorts
      return ""; 
   }

   @Override
   public String getWritePortPreEntrypoint() {
      // no-op for InitializerPorts
      return "";
   }

   @Override
   public String getWritePortPostEntrypoint() {
      // no-op for InitializerPorts
      return "";
   }

   @Override
   public String getWritePortEventResponder() {
      // no-op for InitializerPorts
      return "";
   }

   @Override
   public String getWritePortThreadInitializer() {
      // no-op for InitializerPorts
      return "";
   }

   @Override
   public String getName() {
      return this.getModelElement().getName();
   }

   @Override
   public String getQualifiedName() {
      return this.getModelElement().getQualifiedName();
   }

   public TypeNames getType() { 
      return EmitterFactory.type(this.getModelElement().getType()); 
   }
   
   /************************************************************
    * 
    * VxWorks-specific functions (implementing RPCEventDataPortCamkes)
    * 
    ************************************************************/

   
   @Override
   public String vxWorksAddCommonHFileDeclarations() {
      // no-op for InitializerPorts
      return "";
   }

   @Override
   public String getVxWorksAddMainCFileIncludes() {
      // no-op for InitializerPorts
      return "";
   }

   @Override
   public String getVxWorksAddMainCFileDeclarations() {
      // no-op for InitializerPorts
      return "";
   }

   @Override
   public String getVxWorksAddMainCFileInitializers() {
      // thread initialization routines (if any)...
      String result = ""; 
      result += "int64_t dummy_time = 0;\n";
      result += this.getActiveThreadInternalDispatcherFnName() + "(&dummy);\n";
      return result; 
   }

   @Override
   public String getVxWorksAddMainCFileDestructors() {
      return "";
   }

   /************************************************************
    * 
    * eChronos-specific functions (implementing RPCEventDataPortCamkes)
    * 
    ************************************************************/

   public String getEChronosAddPrxMutexes() {
      return "";
   }
   
   public String getEChronosAddPrxSemaphores() {
      return "";
   }

   @Override
   public String getEChronosAddCommonHFileDeclarations() {
      return "";
   }

   @Override
   public String getAddTrampolines() { return ""; }
   
   @Override
   public String getAddInternalIrqs() { return ""; }
   
   @Override
   public String getAddExternalIrqs() { return ""; }
   
   /************************************************************
    * 
    * CAmkES-specific functions (implementing RPCDataPortCamkes)
    * 
    ************************************************************/

   @Override
   public String getAddComponentPortLevelDeclarations() {
      return ""; 
   }

   @Override
   public String getAddAssemblyFileCompositionPortDeclarations() {
      return ""; 
   }
   
   @Override
   public String getAddAssemblyFileConfigDeclarations() {
      return "";
   }
   
   @Override
   public String getAddAssemblyFilePortDeclarations() { return ""; }

   /************************************************************
    * 
    * Names functions used by the .stg templates for input queues
    *
    * type
    * name
    * incomingPortWriterName <--> incomingWriterName --> OS-specific
    * incomingUserEntrypointCallerName <--> entrypointCallerName --> OS-specific
    * activeThreadInternalDispatcherFnName
    * 
    * the entrypointCallerName is only dispatched remotely for 
    *    passive threads.  For active threads it will always be 
    *    dispatched from the main thread dispatch loop.
    * 
    * 
    ************************************************************/

   public String getPrefix() { return Util.getPrefix(); }
   
   public String getIdlDispatcherName() {
      return "dispatch_" + getName();
   }

   public String writeType() {
      return "_write" + getModelElement().getType().getCType().typeString();
   }

   public String getLpcPortWriterName() {
      return Util.getPrefix_() + getModelElement().getQualifiedName() + 
            writeType();
   }

   // middleware functions; these must be compatible with the OS.
   public String getIncomingWriterName() {
      PortFeature dp = getModelElement();
      if (model.getOsTarget() == OSModel.OSTarget.CAmkES) {
         return dp.getName() + writeType() ;
      } else if (model.getOsTarget() == OSModel.OSTarget.eChronos) {
         return getLpcPortWriterName();
      } else if (model.getOsTarget() == OSModel.OSTarget.VxWorks) {
         return getLpcPortWriterName();
      } else {
         throw new TbException("Error: getIncomingPortWriterName: OS " + model.getOsTarget() + " is not a known OS target.");
      }
   }

   public boolean getHasData() { 
      return !(this.getModelElement().getType() instanceof UnitType); 
   }

   public String getUserEntrypointName() {
      if (this.model.getOsTarget() == OSModel.OSTarget.CAmkES){
         ThreadImplementationNames tnames = 
               EmitterFactory.threadImplementation(this.getModelElement().getOwner());
         String name = tnames.getComponentDispatcherInterfaceVarIdName() + "_" + 
                     this.getIdlDispatcherName() ; 
         return name;
      } else if (this.model.getOsTarget() == OSModel.OSTarget.eChronos) {
         return this.getPrefix() + "_entrypoint_" + 
                this.getQualifiedName(); 
      } else if (this.model.getOsTarget() == OSModel.OSTarget.VxWorks) {
         return this.getIncomingWriterName();
      } else {
         throw new TbException("in RPCEventDataPortEmitter::getUserEntrypointName: OS [" + 
               this.model.getOsTarget() + "] is not supported.");
      }
   }
   
   public String getActiveThreadInternalDispatcherFnName() {
      return getUserEntrypointName(); 
   }
}
