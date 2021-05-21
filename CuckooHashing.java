import java.util.*;

/**
 * Cuckoo hash table implementation of hash tables.
 *
 */
public class CuckooHashing {
    private static final int DEFAULT_TABLE_SIZE = 101;
    
    private final HashMethods hashFunctions;
    private final int numHashFunctions;
    
    private String[] array; // The array of elements
    private int currentSize; // The number of occupied cells
    private ArrayList<String> stash; //List of items that couldn't find a place
    private Stack<String []> st;
    /**
     * Construct the hash table.
     */
    public CuckooHashing(HashMethods hf) {
        this(hf, DEFAULT_TABLE_SIZE);
    }

    /**
     * Construct the hash table.
     *
     * @param hf   the hash family
     * @param size the approximate initial size.
     */
    public CuckooHashing(HashMethods hf, int size) {
        allocateArray(nextPrime(size));
        stash = new ArrayList<String>();
        makeEmpty();
        hashFunctions = hf;
        numHashFunctions = hf.getNumberOfFunctions();
        st= new Stack<>();       // includes all the undo expressions
    }
    
    /**
     * Insert into the hash table. If the item is
     * already present, return false.
     *
     * @param x the item to insert.
     */
    public boolean insert(String x) {
    	if(this.capacity()==this.size()){
    		return false;
    	}
        if (find(x))
            return false;

        return insertHelper1(x);
    }
    
    private boolean insertHelper1(String x) {
        while (true) {
            int pos = -1;
            int kick_pos = -1;
            // Initializing  a new Expression
            String [] currExp=new String[3];
            currExp[1]=x;
            currExp[1]=String.valueOf(pos);
            currExp[2]=String.valueOf(kick_pos);
            boolean insertedExp = true; // Indicate whether it is the string that you insert or not
            
            //This is NOT part of a real cuckoo hash implementation
            //but is necessary to avoid randomization so we can test your work
            ArrayList<ArrayList<String>> cycle_tester = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.capacity();i++){
            	cycle_tester.add(i, new ArrayList<String>());
            }
            boolean cycle=false;
            
            int MAXTRIES  = this.size();
            for (int count = 0; count <= MAXTRIES; count++) {
                for (int i = 0; i < numHashFunctions; i++) {
                    pos = myhash(x, i);
                    if(isCycle(cycle_tester,x,pos))
                    {
                    	cycle=true;
                    	break;
                    }
                    cycle_tester.get(pos).add(x);
                    if (array[pos] == null) {
                        array[pos] = x;
                        // updates for undo method
                        currExp[0]=x;
                        currExp[1]=String.valueOf(pos);
                        currExp[2]=String.valueOf(kick_pos);
                        st.push(currExp);
                        //
                        currentSize++;
                        return true;
                    }
                    
                } 
                if(cycle)
                	break;
                if(pos==kick_pos || kick_pos==-1)
                	kick_pos= myhash(x, 0);
				else {
					kick_pos=pos;
					insertedExp = false; // for undo method - the position is changing so now it isn't the element which was inserted (just for -1 in [2])
				}
                // none of the spots are available, kick out item in kick_pos
                
                // update for undo method
                currExp[0]=x;
                currExp[1]=String.valueOf(kick_pos);
                if (insertedExp) {
                	currExp[2]="-1";
                	insertedExp = false;
                } else
                	currExp[2]= st.peek()[1]; 
                // coping to a new string[] in order to preserve the right pointers
                String [] newone=new String[3];
                newone[0]=currExp[0];newone[1]=currExp[1];newone[2]=currExp[2]; 
                st.push(newone);
                //
                
                String tmp = array[kick_pos];
                array[kick_pos] = x;
                x = tmp;
            }
            //insertion got into a cycle use overflow list
            this.stash.add(x);
           
            // updates for undo method
            currExp[0]=x;
            currExp[1]="S";
            currExp[2]=String.valueOf(kick_pos);
            st.push(currExp);
            //
            return true;
            
        }
    }

    private boolean isCycle(ArrayList<ArrayList<String>> cycle_tester,String x,int i) {
    	return cycle_tester.get(i).contains(x);
    }
	
	public void undo() {
       if(!st.isEmpty()){
           String [] currExp=st.pop();
           // if the last Exp is located is the array, we need to remove it and update the size
           if(!currExp[1].equals("S")){
        	   int j = Integer.valueOf(currExp[1]);
               array[j]=null;
               currentSize--; // we don't use remove because it clears the stack
           }
           boolean stop = false;
           while(!stop){
        	   // if the Exp is in the stash, first we remove it from stash
               if(currExp[1].equals("S"))
                   stash.remove(currExp[0]);               
               // if the previous location is -1 that means that the Exp was the last one to be inserted
               if (currExp[2].equals("-1"))
        		   break;
               
               array[Integer.valueOf(currExp[2])]=currExp[0]; // switching values
               
               if (!st.isEmpty()) // getting a new Exp if exist
            	   currExp=st.pop();
               else 
            	   stop=true;
           }
    // we "have" the last Exp which was inserted, but we have already switched values in the loop
        
       }
	}
	


    /**
     * @param x the item
     * @param i index of hash function in hash family
     * @return hash value of x using hash function(i) mod table size
     */
    private int myhash(String x, int i) {
        long hashVal = hashFunctions.hash(x, i);

        hashVal %= array.length;
        if (hashVal < 0)
            hashVal += array.length;

        return (int) hashVal;
    }
    
    /**
     * Finds an item in the hash table.
     *
     * @param x the item to search for.
     * @return True iff item is in the table.
     */
    public boolean find(String x) {
        return findPos(x) != -1;
    }
    
    /**
     * Method that searches all hash function places.
     *
     * @param x the item to search for.
     * @return the position where the search terminates or capacity+1 if item is in overflow list, or -1 if not found.
     */
    private int findPos(String x) {
        for (int i = 0; i < numHashFunctions; i++) {
            int pos = (int) myhash(x, i);
            if (array[pos] != null && array[pos].equals(x))
                return pos;
        }
        for(String s:stash) {
        	if(s.equals(x)){
        		return this.capacity()+1;
        	}		
        }

        return -1;
    }

    /**
     * Gets the size of the table.
     *
     * @return number of items in the hash table.
     */
    public int size() {
        return currentSize;
    }

    /**
     * Gets the length (potential capacity) of the table.
     *
     * @return length of the internal array in the hash table.
     */
    public int capacity() {
        return array.length;
    }

    /**
     * Remove from the hash table.
     *
     * @param x the item to remove.
     * @return true if item was found and removed
     */
    public boolean remove(String x) {
        int pos = findPos(x);
        if(pos==-1)
        	return false;
        if (pos<this.capacity()) {
            array[pos] = null;
			currentSize--;
        } else {
        	this.stash.remove(x);
        }
        st.clear();    // updating the current undo options
        return true;
    }

    /**
     * Make the hash table logically empty.
     */
    public void makeEmpty() {
    	currentSize = 0;
        for (int i = 0; i < array.length; i++)
            array[i] = null;
        this.stash.clear();
    }
    
    public String toString() {
    	String ans = "";
    	for (int i = 0; i < capacity(); i++) {
            if (array[i] != null)
                ans = ans.concat("Index: "+ i + " ,String: " +array[i]+"\n");
        }
    	int i=0;
        for(String s:stash) {
        	ans = ans + "Overflow["+ i + "] ,String: " +s+"\n";
        	i++;
        }
        return ans;
    }
    
    /**
     * Method to allocate array.
     */
    private void allocateArray(int arraySize) {
        array = new String [arraySize];
    }

    /**
     * Method to find a prime number at least as large as n.
     */
    protected static int nextPrime(int n) {
        if (n % 2 == 0)
            n++;

        while (!isPrime(n)) {
            n += 2;
        }
        return n;
    }

    /**
     * Method to test if a number is prime.
     */
    private static boolean isPrime(int n) {
        if (n == 2 || n == 3)
            return true;

        if (n == 1 || n % 2 == 0)
            return false;

        for (int i = 3; i * i <= n; i += 2)
            if (n % i == 0)
                return false;

        return true;
    }
	
}