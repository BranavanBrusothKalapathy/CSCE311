
/**
 * Branavan Kalapathy
 * Program4
 * CSCE311
 */


package osp.Memory;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
        for(int i = 0; i < MMU.getFrameTableSize();i++){
            setFrame(i,new FrameTableEntry(i));

        }
    }

    /**
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
                int referenceType, ThreadCB thread)
    {    /*
         * variable declarations
         */
        int x;
        int pageSize;
        int pageID;
        /**
         * calculation of VIrtual address to physical adress
         */
        x = getVirtualAddressBits() - getPageAddressBits();
        pageSize = (int)Math.pow(2,x);
        pageID = memoryAddress/pageSize;
        /**
         * initiate PageTableEntry from Page ID of the page in PTBR.
         */
        PageTableEntry PTentry = getPTBR().pages[pageID];
        /**
         * check if Page entry is valid if so
         * get the frame set referenced frame to true.
         */
        if(PTentry.isValid()){
            PTentry.getFrame().setReferenced(true); 
            /**
             * if refrence type is equal to Memory write, the
             * page table entry wil get its frame and set Dirty to true,
             * cleans up dirty reference.
             * @return PTentry
             */
            if(referenceType == GlobalVariables.MemoryWrite) {
                PTentry.getFrame().setDirty(true);
                return PTentry;

            }
            /**
             * if PTentry is validating thread equals then set intterut type 
             * to refrence type of the interrept vector. set the page
             * of the page table entry of the interrupt vector.
             * set the thread and CPU will page fault the thread.
             * @return PTentry
             */
        }else{
            if(PTentry.getValidatingThread() == null){



                InterruptVector.setInterruptType(referenceType);
                InterruptVector.setPage(PTentry);
                InterruptVector.setThread(thread);
                CPU.interrupt(PageFault);
                if(thread.getStatus() == GlobalVariables.ThreadKill){
                    return PTentry;
                            
                }
            }else{
                /**
                 * suspend the thread waiting on to validate the thread.
                 * get status of thread and prevent Threadkill status.
                 * @return Ptentry
                 */
                thread.suspend(PTentry);
                if(thread.getStatus() == GlobalVariables.ThreadKill){
                    return PTentry;
                }
                /**
                 * if PTentry is valid then get its frame and set
                 * refrence to true. Memory write the entry 
                 * and get its frame and clean dirty frame
                 */
                if(PTentry.isValid()){
                    PTentry.getFrame().setReferenced(true);
                    if(referenceType == GlobalVariables.MemoryWrite)
                        PTentry.getFrame().setDirty(true);
                    return PTentry;
                }
                /**
                 * 2nd check on suspend to wait for validting thread,
                 */
                thread.suspend(PTentry);
                if(thread.getStatus() == GlobalVariables.ThreadKill){
                    return PTentry;
                }



            }
            PTentry.getFrame().setReferenced(true);
            if(referenceType == GlobalVariables.MemoryWrite){
                PTentry.getFrame().setDirty(true);
            
            }

        }

    return PTentry;
    }
    
            
    
        

    

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {
      
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student 
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
