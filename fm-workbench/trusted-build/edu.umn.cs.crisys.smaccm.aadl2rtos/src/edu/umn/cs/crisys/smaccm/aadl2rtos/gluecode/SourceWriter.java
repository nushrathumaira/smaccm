package edu.umn.cs.crisys.smaccm.aadl2rtos.gluecode;

/**
 * @author Mead, Whalen
 * 
 * TODO: many things; most importantly figure out the numbering scheme for events/signals.
 *   For signals, we get either 8, 16, or 32 of 'em (total).
 *   However, we can allocate them on a per-thread basis.
 *   So this means that a thread can have at most 32 dispatchers
 *     (which should be plenty in any system I can imagine).
 *   Each dispatcher (signal) should have an id that is derived from its thread
 *     implementation.
 *   Possibly its position in the dispatcher list.
 *
 * TODO: For events, how do we know that they will be processed?  Suppose they are 
 *   queued (buffered), and we send signals to wake up a thread.  If the thread already has 
 *   waiting events, what do we do?  Do we empty out the event queues one at a 
 *   time? (This may lead to starvation for other queues).  Do we cycle through each queue?
 *
 * TODO: Test regimen: test with queue full, queue empty, queue full/empty at 
 *   size limits.
 * 
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.umn.cs.crisys.smaccm.aadl2rtos.Aadl2RtosException;
import edu.umn.cs.crisys.smaccm.aadl2rtos.Model;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.ArrayType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.IntType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.PointerType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.ast.Type;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.Connection;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.Dispatcher;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.Dispatcher.DispatcherType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ExternalHandler;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.InterruptServiceRoutine;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.MyPort;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.SharedData;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.SharedDataAccessor;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.SharedDataAccessor.AccessType;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ThreadCalendar;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ThreadImplementation;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ThreadInstance;
import edu.umn.cs.crisys.smaccm.aadl2rtos.thread.ThreadInstancePort;
import edu.umn.cs.crisys.smaccm.aadl2rtos.util.Util;

public class SourceWriter extends AbstractCodeWriter {
	// private ArrayList<String> semaphoreList = new ArrayList<String>();
  static final String ind = "   ";

	public SourceWriter(
			BufferedWriter out,
			File CFile,
			File HFile,
			Model model,
			List<MyPort> events) {
		super(out, CFile, HFile, model);
		// this.semaphoreList = model.getSemaphores();
	}

	public void writeSource() throws IOException {
		// Write license data
		writeLicense();
		out.append("\n\n");

		// Write file metadata (date, filename, etc.)
		writeFileMetadata(CFile);
		out.append("\n\n");

		// Write file description
		writeComment("   This .c file contains the implementations of the communication functions for threads \n"
				+ "   and the top level entrypoint functions for the AADL components as defined in the \n"
				+ "   system implementation " + sysInstanceName + ".\n");
		out.append("\n\n");

		// Write include directives
		out.append("#include <stdbool.h>\n");
		out.append("#include <stddef.h>\n");
		out.append("#include <stdint.h>\n");
		//out.append("#include <string.h>\n");
		out.append("#include \"rtos-kochab.h\"\n");
		out.append("#include \"" + HFile.getName() + "\"\n\n");
		
		//memcpy signature.
		writeStupidMemcpy();
		
		out.append("void ivory_echronos_begin_atomic();\n");
		out.append("void ivory_echronos_end_atomic();\n\n");
		out.append("uint64_t timer_get_ticks();\n\n");
		out.append("uint32_t smaccm_get_time_in_ms() {return (uint32_t)(timer_get_ticks() / 1000ULL); }\n\n");
		
		if (model.getThreadCalendar().hasDispatchers()) {
		  writeInitializePX4SystickInterrupt() ;
		}
		// defineMutexes();
    writeAllSharedVars();
    
    if (model.getThreadCalendar().hasDispatchers()) {
      writeThreadCalendar();
    }
    
    writeReaders();
    writeIsEmptys();
    writeWriters();
		writeISRs(); 

		// Write top-level tasking functions
		writeThreadImplMainFunctions();
		
		// Write thread instance functions
		//writeThreadInstanceMainFunctions();

		// Define main function
		defineMainFunction();

		// End of file
		writeComment("   End of autogenerated file: " + CFile.toString() + "\n");
	}
	
	String memcpyStmt(Type ty, String dst, String src) {
    return "memcpy(" + dst + ", " + src +  
        ", sizeof(" + ty.getCType().varString("") + "))";
	}
	
	String writeToAadlMemcpy(Type ty, String sharedDst, String src) {
	  return memcpyStmt(ty, Names.getVarRef(ty, sharedDst), src);
	}
	
  String readFromAadlMemcpy(Type ty, String dst, String sharedSrc) {
    return memcpyStmt(ty, dst, Names.getVarRef(ty,  sharedSrc));
  }
  
	/* 
	 * void *memcpy(void *dst, const void*src, int count) {
	 *   uint32_t i; 
	 *   uint8_t *src_ptr = (uint8_t *)src; 
	 *   uint8_t *dst_ptr = (uint8_t *)dst;
	 *   for (i = 0; i < count; i++) {
	 *     *dst_ptr = *src_ptr;
	 *     dst_ptr++; src_ptr++; 
	 *   }
	 *   return dst;
	 * }
	 */
	void writeStupidMemcpy() throws IOException {
	  out.append("void *memcpy(void *dst, const void *src, int count);\n\n");
/*	  out.append("void *memcpy(void *dst, const void *src, int count) { \n"); 
	  out.append(ind + "uint32_t i;\n");
	  out.append(ind + "uint8_t *src_ptr = (uint8_t *)src;\n"); 
	  out.append(ind + "uint8_t *dst_ptr = (uint8_t *)dst;\n");
	  out.append(ind + "for (i=0; i < count; i++) { \n"); 
	  out.append(ind + ind + "*dst_ptr = *src_ptr;\n");
	  out.append(ind + ind + "dst_ptr++; src_ptr++;\n");
	  out.append(ind + "}\n");
	  out.append(ind + "return dst;\n");
	  out.append("}\n\n");
	  */
	}
	
	
	private void writeInitializePX4SystickInterrupt() throws IOException {
	   
	   int gcd = this.model.getThreadCalendar().getGreatestCommonDivisorInMilliseconds(); 
	   String gcd_str = Integer.toString(gcd); 
	   writeComment("Initialize the systick signal based on the GCD of the thread periods\n");

	  out.append("#define SYST_CSR_REG 0xE000E010         // Basic control of SysTick: enable, clock source, interrupt, or poll\n" + 
               "#define SYST_RVR_REG 0xE000E014         // Value to load Current Value register when 0 is reached\n" + 
               "#define SYST_CVR_REG 0xE000E018         // The current value of the count down\n" +
               "#define SYST_CAV_REG 0xE000E01C         // Calibration value for count down.\n\n" + 
               "#define SYST_CSR_READ() (*((volatile uint32_t*)SYST_CSR_REG))\n" + 
               "#define SYST_CSR_WRITE(x) (*((volatile uint32_t*)SYST_CSR_REG) = x)\n\n" +
               "#define SYST_RVR_READ() (*((volatile uint32_t*)SYST_RVR_REG))\n" + 
               "#define SYST_RVR_WRITE(x) (*((volatile uint32_t*)SYST_RVR_REG) = x)\n\n" + 
               "#define SYST_CVR_READ() (*((volatile uint32_t*)SYST_CVR_REG))\n" + 
               "#define SYST_CVR_WRITE(x) (*((volatile uint32_t*)SYST_CVR_REG) = x)\n" + 
               "#define SYST_CAV_READ() (*((volatile uint32_t*)SYST_CAV_REG))\n\n");
	  
	  out.append("void smaccm_initialize_px4_systick_interrupt() {\n");
	  out.append(ind + "/* The SysTick Calibration Value Register is a read-only register that contains\n" +  
	             ind + "the number of pulses for a period of 10ms, in the TENMS field, bits[23:0].\n" + 
	             ind + "This register also has a SKEW bit. Bit[30] == 1 indicates that the calibration\n" +  
	             ind + "for 10ms in the TENMS section is not exactly 10ms due to clock frequency.\n" +  
	             ind + "Bit[31] == 1 indicates that the reference clock is not provided.*/\n\n");
	  out.append(ind + "uint32_t cav_value = SYST_CAV_READ();\n" + 
	             ind + "uint32_t gcd_value = " + gcd_str + ";                        // SMACCM thread rate GCD in milliseconds \n" + 
	             // ind + "// uint32_t has_skew = cav_value   & 0x40000000 ;   // non-zero if the number of cycles does not exactly match 10 ms\n" + 
	             // ind + "// uint32_t no_ref_val = cav_value & 0x80000000 ;   // non-zero if this value is garbage; there is no reference.\n" + 
	             ind + "uint32_t ten_ms_val = cav_value & 0x00ffffff ;   // number of cycles per 10ms\n" +  
	             ind + "uint32_t one_ms_val = cav_value / 10;            // number of cycles per 1ms\n\n" + 
	             ind + "uint32_t mult_of_ten_ms = gcd_value / 10;\n" + 
	             ind + "uint32_t remainder_of_ten_ms = gcd_value % 10;\n\n"); 

	  out.append(ind + "uint32_t desired_rate = (mult_of_ten_ms * ten_ms_val) + (remainder_of_ten_ms * one_ms_val) ; \n");
	  out.append(ind + "SYST_RVR_WRITE(desired_rate);\n");
    out.append(ind + "SYST_CVR_WRITE(0);\n");
    out.append(ind + "SYST_CSR_WRITE((1 << 1) | 1);\n");
	  out.append("};\n");
	}
	
	// group threads by periods for better performance.
	private void writeThreadCalendar() throws IOException {
	  
	  ThreadCalendar tc = this.model.getThreadCalendar();
	  int hyperperiodSubdivisions = tc.getHyperperiodSubdivisions();
	  int tickInterval = tc.getGreatestCommonDivisorInMilliseconds();

	  writeComment("ISR Function for managing periodic thread dispatch.\n");
	  
	  out.append("int smaccm_calendar_counter = 0;\n\n");
	  out.append("bool smaccm_thread_calendar() {\n");
	  
	  for (Dispatcher d : tc.getPeriodicDispatchers()) {
	    int ctr = d.getPeriod() / tickInterval;
	    out.append(ind + "if ((smaccm_calendar_counter % " + Integer.toString(ctr) + ") == 0) { \n");
      out.append(ind + ind + "rtos_irq_event_raise(" + 
          d.getPeriodicIrqSignalDefine() + ");\n");
      out.append(ind + "}\n");
	  }
    out.append(ind + "smaccm_calendar_counter = (smaccm_calendar_counter + 1) % " + 
        hyperperiodSubdivisions + ";\n");
    out.append(ind + "return true;\n");
	  out.append("}\n\n");
    writeComment("End dispatch.\n");
	}
	
	private void writeISRs() throws IOException {
	  for (InterruptServiceRoutine r: model.getISRList()) {
	    out.append("bool " + r.getHandlerName() + "() { \n\n");
	    if (r.getThreadInstances().size() > 1) {
	      throw new Aadl2RtosException("For SystemBuild translation, Interrupt Service Routines (ISRs)" + 
	          " can have only one thread instance for the corresponding ISR thread implementation.\n");
	    }
	    ThreadInstance ti = r.getThreadInstances().get(0);
	    ThreadInstancePort tip = new ThreadInstancePort(ti, r.getDestinationPort());
        if (model.getISRType() == Model.ISRType.SignalingISR) {
		    out.append(ind + tip.getVarName() + " += 1; \n"); 
		    out.append(ind + "rtos_irq_event_raise(" + 
		        r.getIrqSignalDefine() + ");\n");
        } else if (model.getISRType() == Model.ISRType.InThreadContextISR) {
        	out.append(ind + r.getTowerHandlerName() + "();\n");
        } else {
        	throw new Aadl2RtosException("Unknown ISR Type for ISR: " + r.getIrqSignalName());
        }
	    out.append(ind + "return true;\n");
	    out.append("}\n\n");
	  }
	}
	
	private void defineMainFunction() throws IOException {
//		out.append("int main() {\n");
//		out.append("    int result;\n");

//		for (String semaphore : semaphoreList) { 
//			out.append("    result = sem_init(&" + semaphore + ", 0, 1);\n");
//		}
//		out.append("\n    if (result != 0) {\n");
//		out.append("        perror(\"Semaphore initialization failed\");\n");
//		out.append("        exit(0);\n");
//		out.append("    }\n");
//		out.append("}\n\n");
	}

	
	/*
	 * Right now, the thread main functions do not reference periodic events.
	 * Also, they do not 'pump' event queues, so it is possible that message
	 * notifications can be lost.  What we need to do is as follows:
	 * 
	 * - Start from handlers rather than from ports so that the periodic handler 
	 *   is included.
	 * - When handling input events, we need to pump out the queues.
	 *   
	 */
	private void writeThreadEventDataPortDispatcher(Dispatcher d) throws IOException {
    
	  MyPort p = d.getEventPort();
	  String fnName = Names.getInputQueueIsEmptyFnName(p.getOwner(), p);
    out.append(Util.ind(3) + "while (! " + fnName + "()) {\n");
    Type ty = p.getDataType(); 
    out.append(Util.ind(4) + ty.getCType().varString("elem") + ";\n");
    out.append(Util.ind(4) + Names.getThreadImplReaderFnName(p) + "(" + Names.getVarRef(ty, "elem") + ");\n");
    
    for (ExternalHandler handler: d.getExternalHandlerList()) {
      out.append(Util.ind(4) + handler.getHandlerName() + 
          "(/*threadID, */ " + Names.getVarRef(ty, "elem") + ");\n");
    }
    out.append(Util.ind(3) + "}\n");
	}

	private void writeThreadEventPortDispatcher(Dispatcher d) throws IOException {
    MyPort p = d.getEventPort();
    String fnName = Names.getInputQueueIsEmptyFnName(p.getOwner(), p);
    out.append(Util.ind(3) + "while (! " + fnName + "()) {\n");
    out.append(Util.ind(4) + Names.getThreadImplReaderFnName(p) + "();\n");
    for (ExternalHandler handler: d.getExternalHandlerList()) {
      out.append(Util.ind(4) + handler.getHandlerName() + "(/*threadID*/);\n");
    }   
    out.append(Util.ind(3) + "}\n");
	}

	
	private void writeThreadPeriodicDispatcher(Dispatcher d) throws IOException {
    for (ExternalHandler handler: d.getExternalHandlerList()) {
      //out.append(Util.ind(3) + "millis_from_sys_start += " + Integer.toString(d.getPeriod()) + ";\n");
      out.append(Util.ind(3) + handler.getHandlerName() + "(/*threadID, */ smaccm_get_time_in_ms());\n");
    }	  
	} 
	
	private void writeThreadImplMainFunctions() throws IOException {

		
		List<ThreadImplementation> threads;
//		if (model.getISRType() == Model.ISRType.SignalingISR) {
			threads = allThreads;
//		} else if (model.getISRType() == Model.ISRType.InThreadContextISR) {
//			threads = new ArrayList<ThreadImplementation>();
//			for (ThreadImplementation ti : allThreads) {
//				if (!ti.isISRThread()) {
//					threads.add(ti);
//				}
//			}
//		} else {
//			throw new Aadl2RtosException("Error: unknonwn ISR type: " + model.getISRType().toString());
//		}
				
		for (ThreadImplementation tw : threads) {
			out.append("void " + tw.getGeneratedEntrypoint() + "(/*int threadID*/) \n");
			out.append("{\n");

			// out.append(Util.ind(1) + "uint32_t millis_from_sys_start = 0;\n");

			ExternalHandler initHandler = tw.getInitializeEntrypointOpt();
			if (initHandler != null) {
				out.append(Util.ind(1) + initHandler.getHandlerName() + "(/* threadID */);\n\n");
			}

			out.append(Util.ind(1) + "for (;;) {\n");
			out.append(Util.ind(2) + "int current_sig = " + rtosFnName("signal_wait_set("));

			if (tw.getDispatcherList().size() == 0) {
				throw new Aadl2RtosException("Error: Thread implementation: '" + tw.getName() + "' has no dispatchers!");
			}
			Iterator<Dispatcher> it = tw.getDispatcherList().iterator();
			while (it.hasNext()) {
			  Dispatcher current = it.next();
				int index = current.getOwner().getSignalNumberForDispatcher(current); 
				out.write(Integer.toHexString(1 << index));
				out.append("/*" + current.getName() + "*/");
				if (it.hasNext()) {
					out.write(" | ");
				}
			}
			out.append("); \n");

			// call entrypoint functions
			it = tw.getDispatcherList().iterator();
			boolean initial = true;
			while (it.hasNext()) {
				// second is destination port.
				Dispatcher current = it.next();
				
				out.append(Util.ind(2) + (initial ? "" : "else "));
				out.append("if (current_sig == "
						+ current.getOwner().getSignalNumberForDispatcher(current) + "/*"
						+ current.getName() + "*/" + ") {\n");

				if (current.getDispatcherType() == DispatcherType.INPUT_PORT_DISPATCHER) {
				  if (current.getEventPort().isInputEventDataPort()) {
	          writeThreadEventDataPortDispatcher(current); 				    
				  } else {
				    writeThreadEventPortDispatcher(current); 
				  }
				} else {
				  writeThreadPeriodicDispatcher(current); 
				}
				out.append(Util.ind(2) + "}\n");
				initial = false;
			}
			out.append(ind + "}\n");
			out.append("}\n\n");
		}
	}
	
	public void writeThreadInstanceMainFunctions() throws IOException {

		for (ThreadImplementation tw : allThreads) {
			List<ThreadInstance> instanceList = tw.getThreadInstanceList();
			
			for (ThreadInstance instance: instanceList) {
				out.append("void " + instance.getGeneratedEntrypoint() + "()\n");
				out.append("{\n");
				out.append(ind + tw.getGeneratedEntrypoint() + "(" + instance.getKochabThreadId() + ");\n");
				out.append("}\n\n");
			}
		}
	}

	/* 
	 * How to define the circular buffer for Event-Data ports.
	 *   ...go to wikipedia and make sure you do it correctly.
	 *   Issues: begin and end same.
	 *     If begin = end, queue is empty.
	 *     If begin = end + 1 or end = size - 1 and begin = 0 then queue is full 
	 *     We don't need a size() operator
	 *     To add, if queue is not full, simply add at the end pointer and 
	 *       increment it (mod size).
	 *     To remove, if the queue is not empty, simply add to the begin pointer
	 *       and increment it (mod size).  
	 *   Can I find this code somewhere?
	 */

   private void writeEnterCriticalSection(String ind, String mutexDefine) throws IOException {
      // start critical section
      if (model.getCommMutexPrimitive() == Model.CommMutualExclusionPrimitive.Semaphore) {
    	  out.append(ind + rtosFnName("mutex_lock(") + mutexDefine + ");\n");
      } else {
    	  out.append(ind + "ivory_echronos_begin_atomic(); \n");
      }
   }

   private void writeExitCriticalSection(String ind, String mutexDefine) throws IOException {
      // finish critical section
      if (model.getCommMutexPrimitive() == Model.CommMutualExclusionPrimitive.Semaphore) {
    	  out.append(ind + rtosFnName("mutex_unlock(") + mutexDefine + ");\n");
      } else {
    	  out.append(ind + "ivory_echronos_end_atomic(); \n");
      }
   }
      
   
  private void writeIsEmpty(ThreadImplementation impl, MyPort inp) throws IOException {
    
    String fnName = Names.getInputQueueIsEmptyFnName(impl, inp);
    out.append("bool " + fnName + "(/* THREAD_ID tid,  */ "); 
    out.append(") {\n\n");  

    // create function result variable.
    out.append(ind + "bool result = false;\n\n");
    
    if (impl.getThreadInstanceList().size() != 1) {
      throw new Aadl2RtosException("In current aadl2rtos implementation, only one thread instance is " + 
          "allowed per thread implementation.  Violating thread implementation: " + 
          impl.getName());
    }
    
    //List<ThreadInstancePort> tips = impl.getThreadInstanceInputPorts(); 
    
    for (ThreadInstance ti: impl.getThreadInstanceList()) {
      writeThreadInstanceComment(ti);
      
      ThreadInstancePort tip = new ThreadInstancePort(ti, inp); 
      //int dstThreadId = c.getDestThreadInstance().getThreadId();
      MyPort destPort = tip.getPort();
      
      writeEnterCriticalSection(ind, tip.getMutexDefine());
      
      if (destPort.isInputDataPort()) {
        out.append(ind + "result = false; \n");
      }
      else if (destPort.isInputEventPort()) {
        out.append(ind + "result = " + tip.getVarName() + " == 0;\n");
      }
      else if (destPort.isInputEventDataPort()) {
         out.append(ind + "result = " + tip.getVarName() + "_is_empty();\n");
      }

      // unlock the semaphore
      writeExitCriticalSection(ind, tip.getMutexDefine());
    }
    out.append(ind + "return result;\n");
    out.append("}\n\n");
  }

  private void writeIsEmptys() throws IOException {
    for (ThreadImplementation ti : allThreads) {
      for (MyPort pi : ti.getInputPorts()) {
        writeIsEmpty(ti, pi);
      }
    }
  }

  private void writeThreadInstanceComment(ThreadInstance ti) throws IOException {
    out.append(ind + " /////////////////////////////////////////////////////////////////////////\n");
    out.append(ind + " // here is where we would branch based on thread instance id.\n");
    out.append(ind + " // For now we support only one thread instance per thread implementation.\n");
    out.append(ind + " // In this case we would split on destination thread id: " + 
        ti.getName() + "\n");
    out.append(ind + " /////////////////////////////////////////////////////////////////////////\n");
    out.append("\n\n");    
  }
  
  private void writeReader(ThreadImplementation impl, MyPort inp) throws IOException {
    
    Type argType = inp.getDataType();
    
    out.append("bool " + Names.getThreadImplReaderFnName(inp) 
        + "(/* THREAD_ID tid,  */ "); 
    if (inp.isInputEventPort()) {
      out.append(") {\n\n");  
    } else {
      out.append(Names.createRefParameter(argType, "elem") + ") {\n\n");
    }
    // create function result variable.
    out.append(ind + "bool result = true;\n\n");
    
    if (impl.getThreadInstanceList().size() != 1) {
      throw new Aadl2RtosException("In current aadl2rtos implementation, only one thread instance is " + 
          "allowed per thread implementation.  Violating thread implementation: " + 
          impl.getName());
    }
    for (ThreadInstance ti: impl.getThreadInstanceList()) {
      writeThreadInstanceComment(ti);
      
      ThreadInstancePort tip = new ThreadInstancePort(ti, inp); 
      MyPort destPort = tip.getPort();
      Type destPortType = destPort.getDataType();
      
      // lock the semaphore
      writeEnterCriticalSection(ind, tip.getMutexDefine());
      
      if (destPort.isInputDataPort()) {
        if (destPortType.isBaseType()) {
          out.append(ind + "*elem = " + tip.getVarName() + "; \n");
        } else {
          out.append(ind + this.readFromAadlMemcpy(destPortType, "elem", tip.getVarName()) + ";\n");
        }
      }
      else if (destPort.isInputEventPort()) {
        out.append(ind + "if (" + tip.getVarName() + " > 0) {\n");
          out.append(ind + ind + tip.getVarName() + " -= 1; \n");
        out.append(ind + "} else {\n");
          out.append(ind + ind + "result = false;\n");
        out.append(ind + "}\n");
      }
      else if (destPort.isInputEventDataPort()) {
        // correct algorithm involves: 
        //
        out.append(ind + "result = " + tip.getVarName() + "_dequeue(elem);\n");
      }
      // unlock the semaphore
      writeExitCriticalSection(ind, tip.getMutexDefine());
    }
    out.append(ind + "return result;\n");
    out.append("}\n\n");
  }

  private void writeSharedDataReader(ThreadImplementation impl, SharedDataAccessor outp) throws IOException {
    SharedData sharedData = outp.getSharedData();
    Type dt = sharedData.getDataType();
    out.append("bool " + Names.getThreadImplReaderFnName(outp) + 
      "(/* THREAD_ID tid,  */ " + 
        Names.createRefParameter(outp.getSharedData().getDataType(), "elem") + ") {\n\n");
    // unlock the semaphore
    out.append(ind + "bool result = true;\n\n");
    writeEnterCriticalSection(ind, outp.getSharedData().getMutexDefine()); 
    if (dt.isBaseType()) {
      out.append(ind + "*elem = " + sharedData.getVarName() + "; \n");
    } else {
      out.append(ind + this.readFromAadlMemcpy(dt, "elem", sharedData.getVarName()) + ";\n");
    }
    writeExitCriticalSection(ind, outp.getSharedData().getMutexDefine()); 
    out.append(ind + "return result;\n");
    out.append("}\n\n");
  }
  
  private void writeReaders() throws IOException {
    for (ThreadImplementation ti : allThreads) {
      for (MyPort pi : ti.getInputPorts()) {
        writeReader(ti, pi);
      }
      for (SharedDataAccessor sda: ti.getSharedDataAccessors()) {
        if (sda.getAccessType() == AccessType.READ_WRITE || 
            sda.getAccessType() == AccessType.READ) {
          writeSharedDataReader(ti, sda);
        }
      }
    }
  }

  String StringForNonArrayType(Type t, String ret) {
    if (t instanceof ArrayType) {
      return ""; 
    } else {
      return ret; 
    }
  }
  
  private void writeEventDataPortSharedVars(ThreadInstancePort c, MyPort dstPort, Type portTy) throws IOException {
    String arraySize = Integer.toString(c.getArraySize());
    // TODO: fix this.
    String queueName = c.getVarName();
    String isFullName = c.getIsFullName();
    String head = c.getCircBufferFrontVarName();
    String tail = c.getCircBufferBackVarName();

    out.append(c.getQueueType().getCType().varString(queueName) + "; \n");
    out.append("bool " + isFullName + " = false; \n");
    out.append(c.getCircRefType().getCType().varString(head) + "; \n");
    out.append(c.getCircRefType().getCType().varString(tail) + "; \n\n");
    
    // right now we can only support queues 
    // if (queueSize >= )
    
    // Write is_full function
    out.append("bool " + queueName + "_is_full() {\n");
    out.append(ind + "return (" + tail + " == " + head + ") && (" + isFullName + ");\n");
    out.append("}\n\n");

    // Write is_empty function
    out.append("bool " + queueName + "_is_empty() {\n");
    out.append(ind + "return (" + tail + " == " + head + ") && (!" + isFullName + ");\n");
    out.append("}\n\n");

    // Write enqueue function
    out.append("bool " + queueName + "_enqueue(const " + Names.createRefParameter(portTy, "elem") + ") {\n");
    out.append(ind + "if (" + queueName + "_is_full()) {\n");
    out.append(ind + ind + "return false;\n");
    out.append(ind + "} else {\n");


    if (portTy.isBaseType()) {
      out.append(ind + ind + queueName + "[" + tail + "] = *elem;\n");
    }
    else {
      out.append(ind + ind + 
          writeToAadlMemcpy(portTy, queueName + "[" + tail + "]", "elem") + ";\n");
    }

    out.append(ind + ind + tail + " = (" + tail + " + 1) % " + arraySize + ";\n");
    out.append(ind + ind + "if (" + tail + " == " + head + ") { " + isFullName + " = true; } \n\n");
    out.append(ind + ind + "return true;\n");
    out.append(ind + "}\n");
    out.append("}\n\n");

    // Write dequeue function
    out.append("bool " + queueName + "_dequeue(" + Names.createRefParameter(portTy, "elem") + ") {\n");
    out.append(ind + "if (" + queueName + "_is_empty()) {\n");
    out.append(ind + ind + "return false;\n");
    out.append(ind + "} else {\n");
    if (portTy.isBaseType()) {
      out.append(ind + ind + "*elem = " + queueName + "[" + head + "] ;\n");
    }
    else {
      out.append(ind + ind + 
          readFromAadlMemcpy(portTy, "elem", queueName + "[" + head + "]") + ";\n");
    }
    out.append(ind + ind + head + " = (" + head + " + 1) % " + arraySize + ";\n");
    out.append(ind + ind + isFullName + " = false; \n");
    out.append(ind + ind + "return true;\n");
    out.append(ind + "}\n");
    out.append("}\n\n");
  }
  
  private void writeThreadInstancePortSharedVars(ThreadInstancePort c) throws IOException {
	  
	  MyPort dstPort = c.getPort();
	  Type portTy = dstPort.getDataType();
	  
    writeComment("Shared data for thread instance port: " + c.getNameRoot() + "\n");
    if (dstPort.isInputDataPort()) {
	    out.append(portTy.getCType().varString(c.getVarName()) + "; \n");
	  } else if (dstPort.isInputEventDataPort()) {
	    writeEventDataPortSharedVars(c, dstPort, portTy);
	  } else if (dstPort.isInputEventPort()) {
	    Type countType = new IntType(32, false);
	    out.append(countType.getCType().varString(c.getVarName()) + "; \n");
	  } else {
	    throw new Aadl2RtosException("When writing connection variables, destination " + 
	        " port type is not one of: {Data, Event, Event Data} \n");
	  }
	  out.append("\n\n");
	}

  // TODO: normalize ports and shared data in terms of functions.
  private void writeSharedDataVar(SharedData c) throws IOException {
    writeComment("Shared data for shared data port: " + c.getPortName() + "\n");
    out.append(c.getDataType().getCType().varString(c.getVarName()) + "; \n");
  }
  
  private void writeAllSharedVars() throws IOException {
    writeComment("Shared variables for port-to-port communication.\n");
    for (ThreadInstancePort c: this.model.getAllThreadInstanceInputPorts()) {
      writeThreadInstancePortSharedVars(c);
    }
    out.append("\n");
    for (SharedData c: this.model.getSharedDataList()) {
      writeSharedDataVar(c);
    }
  }
  
  // Here's what we want to do.  First, we want use the thread instance id to 
  // determine which connection we write to.  Next, we grab the semaphore 
  // associated with the connection.  Then we perform an appropriate write 
  // action to the shared data; If it is a data port, we update the shared 
  // data.  If this is an event port, we increment the associated pending 
  // event counter.  If it is an event/data port, we add the data to the 
  // array representing the queue.  Finally, if the port is an event port, 
  // we send a signal to the target thread. 
  

  private void writeWriter(ThreadImplementation impl, MyPort outp) throws IOException {
    
    Type argType = outp.getDataType();
    
    out.append("bool " + Names.getThreadImplWriterFnName(outp) + 
        "(/* THREAD_ID tid,  */ "); 
    if (outp.isOutputEventPort()) {
      out.append(") {\n\n");  
    } else {
      out.append("const " + Names.createRefParameter(argType, "elem") + ") {\n\n");
    }

    // create function result variable.
    out.append(ind + "bool result = true;\n\n");
    
    if (impl.getThreadInstanceList().size() != 1) {
      throw new Aadl2RtosException("In current aadl2rtos implementation, only one thread instance is " + 
          "allowed per thread implementation.  Violating thread implementation: " + 
          impl.getName());
    }
    for (ThreadInstance ti: impl.getThreadInstanceList()) {
      writeThreadInstanceComment(ti);
      
      
      // for (ThreadInstance ti: impl.getThreadInstanceList()) {
      // }
      for (Connection c: outp.getConnections()) {
        MyPort destPort = c.getDestPort();
        Type destPortType = destPort.getDataType();
        ThreadInstance destThread = c.getDestThreadInstance();
        ThreadInstancePort tip = new ThreadInstancePort(destThread, destPort); 
        
        /*
        out.append(ind + " // here is where we would branch based on thread instance id.\n");
        out.append(ind + " // For now we support only one thread instance per thread implementation.\n");
        out.append(ind + " // In this case we would split on source thread id: " + 
            Integer.toString(srcThreadId));
        out.append("\n\n");
        */
        
        // lock the semaphore
        writeEnterCriticalSection(ind, tip.getMutexDefine());
    
        if (destPort.isInputDataPort()) {
          if (destPortType.isBaseType()) {
            out.append(ind + tip.getVarName() + " = *elem; \n");
          } else {
            out.append(ind + 
                this.writeToAadlMemcpy(destPortType, tip.getVarName(), "elem") + ";\n");
          }
        }
        else if (destPort.isInputEventPort()) {
          out.append(ind + tip.getVarName() + " += 1; \n"); 
        }
        else if (destPort.isInputEventDataPort()) {
          out.append(ind + "result = " + tip.getVarName() + "_enqueue(elem);\n");
        }
        if (destPort.isInputEventPort() || destPort.isInputEventDataPort()) {
          int signalNumber = destPort.getPortID(); 
          if (signalNumber != -1) {
            out.append(ind + rtosFnName("signal_send_set(")
                + c.getDestThreadInstance().getKochabThreadId() + 
                ", " + Integer.toString(1 << signalNumber) + 
                "/* " + destPort.getName() + " */); \n");
          } else {
            // TODO: Log to the logger.
          }
        }
        
        // unlock the semaphore
        writeExitCriticalSection(ind, tip.getMutexDefine());
      }
    }
    out.append(ind + "return result;\n");
    out.append("}\n\n");
  }

  private void writeSharedDataWriter(ThreadImplementation impl, SharedDataAccessor outp) throws IOException {
    SharedData sharedData = outp.getSharedData();
    Type dt = sharedData.getDataType();
    out.append("bool " + Names.getThreadImplWriterFnName(outp) + 
      "(/* THREAD_ID tid,  */ "); 
    out.append("const " + Names.createRefParameter(outp.getSharedData().getDataType(), "elem") + ") {\n\n");
    // unlock the semaphore
    out.append(ind + "bool result = true;\n\n");
    writeEnterCriticalSection(ind, outp.getSharedData().getMutexDefine());
    if (dt.isBaseType()) {
      out.append(ind + sharedData.getVarName() + " = *elem; \n");
    } else {
      out.append(ind + 
          this.writeToAadlMemcpy(dt, sharedData.getVarName(), "elem") + ";\n");
    }    
    writeExitCriticalSection(ind, outp.getSharedData().getMutexDefine());
    out.append(ind + "return result;\n");
    out.append("}\n\n");
  }
    
  
	private void writeWriters() throws IOException {
	  out.append("\n /* Writer functions for port-to-port communication.*/\n\n");
		for (ThreadImplementation ti : allThreads) {
			// TODO: check on events: remember we only get 32 total, but they
			// can be different for each thread.
			for (MyPort pi : ti.getOutputPorts()) {
			  writeWriter(ti, pi);
			}
			for (SharedDataAccessor sda: ti.getSharedDataAccessors()) {
			  if (sda.getAccessType() == AccessType.READ_WRITE || 
			      sda.getAccessType() == AccessType.WRITE) {
			    writeSharedDataWriter(ti, sda);
			  }
			}
		}
	}
}