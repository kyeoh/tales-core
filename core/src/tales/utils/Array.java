package tales.utils;




import java.util.ArrayList;
import java.util.HashSet;




public final class Array {



	
	public static ArrayList<String> removeDuplicates(ArrayList<String> array){
        HashSet<String> hashSet = new HashSet<String>(array);
        return new ArrayList<String>(hashSet);
    }
    
}
