/**
 * 
 */
package net.wissel.vertx.proxy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SINLOANER8
 *
 */
public class FilterSelectorTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FilterSelectorTest fst = new FilterSelectorTest();
		fst.runTest1();
		fst.runTest2("");
		fst.runTest2("/");
		fst.runTest2("one/two");
	}

	private void runTest1() {
		String regex = ".*listViewManagerGrid\\.ListViewManagerGrid\\.getRecordLayoutComponent.*";
		String source = "https://ap5.lightning.force.com/aura?r=112&ui-force-components-controllers-lists-listViewDataManager.ListViewDataManager.getItems=1&ui-force-components-controllers-lists-listViewManagerGrid.ListViewManagerGrid.getRecordLayoutComponent=1";
		
		Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        System.out.println(matcher.matches());
		
	}

	
	private void runTest2(String path) {
		System.out.print("Test for "+path+"=");
		System.out.println(("".equals(path) || "/".equals(path) || (path.lastIndexOf("/") < 0))
                ? ""
                : path.substring(path.lastIndexOf("/")+1));
		System.out.println("---");
	}
}
