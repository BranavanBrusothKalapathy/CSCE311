/**
 * Branavan Kalapathy
 * Program4
 * CSCE311
 */


package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    /**
     * instatiate size of page of address bits does calculation of bits.
     */
    static final int size = (int)Math.pow(2.0,MMU.getPageAddressBits());
    public PageTable(TaskCB ownerTask)
    {
        super(ownerTask);
        
        /**
         * create an array of new page Table Entry, called pages
         */
          pages = new PageTableEntry[size];
        /**
         * loop through the size of page address of the page.
         */
        for(int i=0;i<size;i++){
                pages[i] = new PageTableEntry(this,i);
    
        }
 //memory_Init();
        
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {   
      
        /**
         * loop through Frame Table size 
         */
        for(int i=0;i<MMU.getFrameTableSize();i++){
         /**
          * initiate Page Table entry of MMU.
          */
           PageTableEntry pg = MMU.getFrame(i).getPage();
           /**
            * check if page is not nulll and gets the task if so 
            get frame of index i set Page to null, dirty bits to false
            and refrence bit to false.
            */
           if(pg != null && pg.getTask() == getTask()){
                MMU.getFrame(i).setPage(null);
                MMU.getFrame(i).setDirty(false);
                MMU.getFrame(i).setReferenced(false);
           /**
            * check if frame  is reserved is equal to the task 
            if so get frame of indexi set it to unreserved task.
            */
          if (MMU.getFrame(i).getReserved() == getTask()){
            MMU.getFrame(i).setUnreserved(getTask());
          }
           }
       
        }
    }

    


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
