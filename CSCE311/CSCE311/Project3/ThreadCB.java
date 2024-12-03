/**Programmer: Branavan Kalapathy
 * CSCE311
 * email: kalapatb@email.sc.edu 
 */

package osp.Threads;

import java.util.Vector;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
 * This class is responsible for actions related to threads, including creating,
 * killing, dispatching, resuming, and suspending threads.
 * 
 * @OSPProject Threads
 */

public class ThreadCB extends IflThreadCB {
    /**
     * 2 Arrays of Generic list declare to represent Active and Expired Queue.
     * 
     */
    static GenericList active_Array[];
    static GenericList expired_Array[];

    /**
     * The thread constructor. Must call
     * 
     * super();
     * 
     * as its first statement.
     * 
     * @OSPProject Threads
     */

    /**
     * This method will be called once at the beginning of the simulation. The
     * student can set up static variables here.
     * 
     * @OSPProject Threads
     */
    public static void init() {
        /**
         * instanatiaton of generic lists of active and expired array. Contains 5 index
         * of queues.
         */
        active_Array = new GenericList[5];
        expired_Array = new GenericList[5];

        for (int i = 0; i < 5; i++) {
            active_Array[i] = new GenericList();
            expired_Array[i] = new GenericList();
        }

    }

    /**
     * Sets up a new thread and adds it to the given task. The method must set the
     * ready status and attempt to add thread to task. If the latter fails because
     * there are already too many threads in this task, so does this method,
     * otherwise, the thread is appended to the ready queue and dispatch() is
     * called.
     * 
     * The priority of the thread can be set using the getPriority/setPriority
     * methods. However, OSP itself doesn't care what the actual value of the
     * priority is. These methods are just provided in case priority scheduling is
     * required.
     * 
     * @return thread or null
     * 
     * @OSPProject Threads
     */
    static public ThreadCB do_create(TaskCB task) {
        // if task is null call dispatch
        if (task == null) {
            dispatch();
            return null;
        }
        // if task exceeds max threads per task call dispatch
        if (task.getThreadCount() >= MaxThreadsPerTask) {
            dispatch();
            return null;
        } else {
            /*
             * otherwise instantiate new thread set task to priortiy and set status to
             * thready ready.
             */
            ThreadCB newThread = new ThreadCB();
            /**
             * new thread is assigned value of 2 and and placed in the expired array.
             */
            newThread.setPriority(2);
            newThread.setStatus(ThreadReady);
            newThread.setTask(task);
            /*
             * if the added thread is new thread fails call dispatch and return null.
             */
            if (task.addThread(newThread) == FAILURE) {
                dispatch();
                return null;
            }

            /*
             * new thread is appended to the Queue, dispatch is called then returns
             * newThread.
             */
            expired_Array[newThread.getPriority()].append(newThread);
            dispatch();
            return newThread;

        }

    }

    /**
     * Kills the specified thread.
     * 
     * The status must be set to ThreadKill, the thread must be removed from the
     * task's list of threads and its pending IORBs must be purged from all device
     * queues.
     * 
     * If some thread was on the ready queue, it must removed, if the thread was
     * running, the processor becomes idle, and dispatch() must be called to resume
     * a waiting thread.
     * 
     * @OSPProject Threads
     */
    public void do_kill() {
        /*
         * If status of thread is ready remove it from expired and active array.
         */

        expired_Array[this.getPriority()].remove(this);
        active_Array[this.getPriority()].remove(this);

        /*
         * If status of thread is running make the memory get the task anda current
         * tread set MMU of PTBR to null and set current thread to null.
         */
        if (getStatus() == ThreadRunning) {
            try {
                if (this == MMU.getPTBR().getTask().getCurrentThread()) {
                    MMU.setPTBR(null);
                    this.getTask().setCurrentThread(null);

                }
            } catch (NullPointerException e) {

            }

        }
        /*
         * remove thread from task, then set the status to kill the thread.
         */
        getTask().removeThread(this);
        setStatus(ThreadKill);
        /* for loop manage table size */
        for (int i = 0; i < Device.getTableSize(); i++) {
            Device.get(i).cancelPendingIO(this);
        } /*
           * give up resources then call dispath
           */
        ResourceCB.giveupResources(this);
        /* if the task and thread count is 0 kill the task. */
        if (getTask().getThreadCount() == 0) {
            getTask().kill();
        }
        dispatch();
    }

    /**
     * Suspends the thread that is currenly on the processor on the specified event.
     * 
     * Note that the thread being suspended doesn't need to be running. It can also
     * be waiting for completion of a pagefault and be suspended on the IORB that is
     * bringing the page in.
     * 
     * Thread's status must be changed to ThreadWaiting or higher, the processor set
     * to idle, the thread must be in the right waiting queue, and dispatch() must
     * be called to give CPU control to some other thread.
     * 
     * @param event - event on which to suspend this thread.
     * 
     * @OSPProject Threads
     */

    public void do_suspend(Event event) {
        /* current status assigned to get status */
        int currentStatus = this.getStatus();
        /*
         * if status of thread is running manage PTBR and get the task and current
         * thread to set to this. if so set PTBR to null and get the task set its thread
         * to null
         */
        if (currentStatus == ThreadRunning) {
            if (MMU.getPTBR().getTask().getCurrentThread() == this) {
                MMU.setPTBR(null);
                this.getTask().setCurrentThread(null);

            }
            setStatus(ThreadWaiting);
            /*
             * otherwise th ais status is waiting set status and gets status increment 1.
             */
        } else if (currentStatus >= ThreadWaiting) {
            this.setStatus(this.getStatus() + 1);
        }
        /*
         * if Queue is containted this remove it from queue.
         */
        active_Array[getPriority()].remove(this);
        expired_Array[getPriority()].remove(this);

        /* add thread to the event then dispatch */
        event.addThread(this);
        dispatch();
    }

    /**
     * Resumes the thread.
     * 
     * Only a thread with the status ThreadWaiting or higher can be resumed. The
     * status must be set to ThreadReady or decremented, respectively. A ready
     * thread should be placed on the reaSdy queue.
     * 
     * @OSPProject Threads
     */

    public void do_resume() {
        /* if status is threadWaiting print out message to resume */
        if (getStatus() < ThreadWaiting) {
            MyOut.print(this, "Attempt to resume" + this + ",which wasn't waiting");
            dispatch();
            return;
        }
        MyOut.print(this, "Resuming" + this);
        /*
         * if status of thread is still waiting set status to ready.othewise decrement
         * the status of the queue. when inserting a thread into the readyQueue, if the
         * priority of the thread is not already at the highest level (0), then it is
         * raised by decrementing it.The thread is then placed in the appropriate queue
         * of the expired array
         */
        if (getStatus() == ThreadWaiting) {
            setStatus(ThreadReady);
            if (this.getPriority() > 0) {
                setPriority(getPriority() - 1);
            }
            expired_Array[getPriority()].append(this);

        } else if (getStatus() > ThreadWaiting)
            setStatus(getStatus() - 1);

        /*
         * if status is ready the append thread to queue and call dispatch
         */
        dispatch();
    }

    /**
     * Selects a thread from the run queue and dispatches it.
     * 
     * If there is just one theread ready to run, reschedule the thread currently on
     * the processor.
     * 
     * In addition to setting the correct thread status it must update the PTBR.
     * 
     * @return SUCCESS or FAILURE
     * 
     * @OSPProject Threads
     */

    public static int do_dispatch() {
        /* instantiate thread and assign to null */
        ThreadCB thread = null;

        /*
         * try the thread to assign to MMU of PTBR get its task and current thread
         */
        try {

            thread = MMU.getPTBR().getTask().getCurrentThread();

        }

        /*
         * catch null pointer exception
         */
        catch (NullPointerException e) {
        }
        /*
         * if thread is null get its task and set the current thread to null. Then get
         * its task and set the thread to null and PTBR to null of MMU. Set its status
         * to ready and append the thread to the Queue. calling HTimer.get(). This
         * returns the time left until the timer interrupts. If the return value is less
         * than 1, then the timer has interrupted indicating that this thread has
         * exceeded its quantum. If this is the case, then lower the priority of the
         * thread by incrementing its priority value. Then insert it into the
         * appropriate queue of the expired array.
         */

        if (thread != null) {
            if (HTimer.get() < 1 && thread.getPriority() < 4) {
                thread.setPriority(thread.getPriority() + 1);
            }
            expired_Array[thread.getPriority()].append(thread);
            thread.getTask().setCurrentThread(null);
            MMU.setPTBR(null);
            thread.setStatus(ThreadReady);
        }
        /**
         * When do_dispatch()is called, the active array is dispatched. start with the
         * highest priority queue (priority 0). If it is empty, then look at the next
         * highest priority queue. Continue until either you find a thread to dispatch
         * or you determine that the active array is completely empty. If the active
         * array is completely empty, then swap the active array and the expired array
         * and start the search for a thread to launch all over again. Only if both the
         * active array and the expired array are empty should you idle the CPU by
         * setting PTBR to null and returning FAILURE
         * 
         *
         */

        GenericList[] holder;
        thread = arrayCheck();
        if (thread == null) {
            holder = active_Array;
            active_Array = expired_Array;
            expired_Array = holder;
            thread = arrayCheck();
            if (thread == null) {
                MMU.setPTBR(null);
                return FAILURE;
            }
        }

        MMU.setPTBR(thread.getTask().getPageTable());
        thread.getTask().setCurrentThread(thread);
        thread.setStatus(ThreadRunning);
        /*
         * threads with priority values in the range 0-2 should dispatched with a
         * quantum value of 40 and threads with priority values in the range 3-4 should
         * be dispatched with a quantum value of 20.
         */

        if (thread.getPriority() < 3) {
            HTimer.set(40);
        } else {
            HTimer.set(20);
        }

        return SUCCESS;

    }

    /**
     * Called by OSP after printing an error message. The student can insert code
     * here to print various tables and data structures in their state just after
     * the error happened. The body can be left empty, if this feature is not used.
     * 
     * @OSPProject Threads
     *
     **/
    /**
     * helper method called arrayCheck(). return the next thread from the active
     * array or null if the active array is empty.
     * 
     */
    public static ThreadCB arrayCheck() {
        for (int i = 0; i != 5; i++) {
            if (!active_Array[i].isEmpty()) {
                return (ThreadCB) active_Array[i].removeHead();
            }
        }
        return null;
    }

    public static void atError() { // your code goes here ?
    }

    /**
     * Called by OSP after printing a warning message. The student can insert code
     * here to print various tables and data structures in their state just after
     * the warning happened. The body can be left empty, if this feature is not
     * used.
     * 
     * @OSPProject Threads
     * 
     **/
    public static void atWarning() { // your code goes here //

    }
    /*
     * Feel free to add methods/fields to improve the readability of your code
     */
}
/*
 * Feel free to add local classes to improve the readability of your code
 */
