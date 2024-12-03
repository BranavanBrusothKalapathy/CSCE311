
/**
 * Branavan Kalapathy
 * Program4
 * CSCE311
 */


package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{ 
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */

    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
         super(ownerPageTable,pageNumber);

    }

    /**
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
     /**
      * ThreadCB instantiated into 2 threads called th1 and valid thread.
      */
    {   
       ThreadCB th1 = iorb.getThread();
       ThreadCB validThread = getValidatingThread();
       /**
        * check if thread is valid.
        */
       if (!this.isValid()){
          /**
           *  if there is not a valid thread that causes pagefault on page.
           */
          if(validThread == null){
             /**
              * initiate page fault handler.
              */
            PageFaultHandler.handlePageFault(th1, MemoryLock, this);
          }
          /**
           * check if th1 is a vailid thread then suspend if not
           * return FAILURE
           */
          else if(th1 != validThread) {
             iorb.getThread().suspend(this);
             if(!this.isValid()){
               return FAILURE;
          }
       }

    }
    /**
     * get the the status thread th1, to prevent ThreadKill status
     * increment lock count and then return success if thread is not valid return 
     * FAILURE
     */
    if (!(th1.getStatus()== ThreadKill)){
      this.getFrame().incrementLockCount();
      return SUCCESS;
    }
      return FAILURE;
    
   }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {    
       /**
        * if the frame of lock count is greater than zero, 
        then decrement lock count.
        */
       if(this.getFrame().getLockCount() > 0){
            this.getFrame().decrementLockCount();
    }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
