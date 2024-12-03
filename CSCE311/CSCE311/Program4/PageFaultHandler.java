/**
 * Branavan Kalapathy
 * Program4
 * CSCE311
 */



package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
        /**
         * intiate task and get thread task
         */
        TaskCB tsk = thread.getTask();
        /**
         * check if page is valid 
         * @return FAILURE
         */
        if(page.isValid()){
            return FAILURE;

        }
        /**
         * intate frame to null and assign to the helper method
         * NewFrameAccess.
         */
        FrameTableEntry Frame  = null;
        Frame = NewFrameAccess();
        /**
         * check if frame is null if it is
         * there is not enough memory.
         * @return NotEnough memory
         */
        if(Frame == null){
            return NotEnoughMemory;
        }
        /*
        * intiate system event when Page Fault has
        occured
         */
        SystemEvent evnt = new SystemEvent("PageFault occurence");
        /**
         * suspend the event and set the thread to valid and
         * frame to reserved of the task.
         */
        thread.suspend(evnt);
        page.setValidatingThread(thread);
        Frame.setReserved(tsk);
        /**
         * if frame of the page is not equal to
         * null intiate PageTable Entry of that frame
         */
        if(Frame.getPage() != null){
            PageTableEntry ptEntry = Frame.getPage();
            /**
             * if frame is dirty swap the thread of that
             * frame
             */
            if(Frame.isDirty()){
                SwapOutward(thread, Frame);
            /**
             * if thread status is equal to Thread kill
             * notify the event and page and then
             * dispatch it
             * /**
             * @return FAILURe
             *  */
             
                if(thread.getStatus() == GlobalVariables.ThreadKill){
                    page.notifyThreads();
                    evnt.notifyThreads();
                    ThreadCB.dispatch();
                    return FAILURE;
                }
                /**
                 * set frame that is not dirty to false
                 */
                Frame.setDirty(false);
            
            }
            /**
             * set refrence of frame to false and set page to null
             * set page table entry to false when its valid and frame to null
             * 
             */
            Frame.setReferenced(false);
            Frame.setPage(null);
            ptEntry.setValid(false);
            ptEntry.setFrame(null);
            

        
        }
        /**
         * set the Frame of the page to its corresponding
         * frame. Swape page and frame.
         */
        page.setFrame(Frame);
        SwapInward(thread,page);
        /**
         * if thread's status is thread kill
         * set Frame of that page to if is not null
         * set page to null of that frame of threads task
         * i
         */
        if(thread.getStatus() == ThreadKill){
            if(Frame.getPage() != null){
                if(Frame.getPage().getTask() == thread.getTask()){
                    Frame.setPage(null);
                }
            }
            /**
             * notfiy valid threads that page is null
             * set frames to null of that page and the event
             * is dispatch
             * @return FAILURE
             */
            page.notifyThreads();
            page.setValidatingThread(null);
            page.setFrame(null);
            evnt.notifyThreads();
            ThreadCB.dispatch();
            return FAILURE;
        }
        Frame.setPage(page);
        page.setValid(true);

        Frame.setUnreserved(tsk);
        page.setValidatingThread(null);
        page.notifyThreads();
        evnt.notifyThreads();
        ThreadCB.dispatch();
        return SUCCESS;
    }
    /*
       Method to search for Frames that loops to through the frame table
       of MMU.

    */
    private static FrameTableEntry NewFrameAccess()
    {
    	FrameTableEntry Frame = null;
    	for(int i = 0; i < MMU.getFrameTableSize(); i++){
    		Frame = MMU.getFrame(i);
    		if((Frame.getPage() == null) && (!(Frame.isReserved()) && (Frame.getLockCount() == 0))){
    			return Frame;
    		}
    	}
    	for(int i = 0; i < MMU.getFrameTableSize(); i++){
    		Frame = MMU.getFrame(i);
    		if(!Frame.isDirty() && !Frame.isReserved() && Frame.getLockCount() == 0)
    		{
    			return Frame; 
    		}
    	}
    	for(int i = 0; i < MMU.getFrameTableSize(); i++){
    		Frame = MMU.getFrame(i);
            if(!Frame.isReserved() && Frame.getLockCount() == 0){
    			return Frame; 
    		}
    	}
    	return MMU.getFrame(MMU.getFrameTableSize() - 1);
    }
/**
 * swap in function to sqap thread and page
 * @param thread
 * @param page
 */

public static void SwapInward(ThreadCB thread, PageTableEntry page){
    TaskCB initTsk = page.getTask();
    initTsk.getSwapFile().read(page.getID(), page, thread);



}
/**
 * swap out function to swap thread and frame.
 */

public static void SwapOutward(ThreadCB thread, FrameTableEntry frame){
    PageTableEntry initPage = frame.getPage();
    	TaskCB initTask = initPage.getTask();
    	initTask.getSwapFile().write(initPage.getID(), initPage, thread);

}
}
/*
      Feel free to add local classes to improve the readability of your code
*/
