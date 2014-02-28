/*
 * This is the original version of the program. It uses Dynamic
 * Programming to find the products list and is faster than the 
 * normal implementation.
 * 
 * A hash map is used to store the lowest index of the list from 
 * which there does not exist a combination of n products that sum
 * up to a particular budget. The key for this map is generated using
 * n and the budget from each recursive call.
 * 
 * For all recursive calls with a specific value of n and budget,
 * the key is generated and we check if the map contains a value which
 * is lower than the current index. If yes, we just return from the
 * recursive call. Else, we move forward with the recursive call. If
 * at any recursive call, we find out that there is no combination of
 * products available for the given n and budget, we compute the key
 * for the current iteration and set the key's value as the current
 * index in the hash map.
 * 
 */

package zapposGifts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GiftsDP {

	private static final String KEY = "52ddafbe3ee659bad97fcce7c53592916a6bfd73";
	private static final String BASE_URL = "http://api.zappos.com/";
	private int resultCount, numOfGifts;
	private int retryAttempts;
	private float budget, diffThreshold;
	private ArrayList<Product> pickList = new ArrayList<Product>();
	private ArrayList<Product> inputProductsList = new ArrayList<Product>();
	private ArrayList<List<Product>> outputProductsList = new ArrayList<List<Product>>();
	private HashMap<Integer, Integer> lowestNoMatchFoundIndex = new HashMap<Integer, Integer>();
	
	public class Product {
		private float price;
		private int colorId;
		private String productName;
		private int productId;
		public Product(String s) throws JSONException {
			JSONObject obj = new JSONObject(s);
			String p = (String) obj.get("price");
			p = p.substring(1);
			p = p.replace(",", "");
			this.price = Float.parseFloat(p);
			this.colorId = Integer.parseInt((String) obj.get("colorId"));
			this.productName = (String) obj.get("productName");
			this.productId = Integer.parseInt((String) obj.get("productId"));
		}
		public String toString() {
			String tmpString = "\nProduct ID: "+productId;
			tmpString += "\nProduct Name: "+productName;
			tmpString += "\nPrice: $"+price;
			tmpString += "\nColor ID: "+colorId;
			tmpString += "\nProduct URL: http://www.zappos.com/product/"
					+productId+"/color/"+colorId+"\n";
			return tmpString;
		}
		public float getPrice() {
			return price;
		}
		public void setPrice(float price) {
			this.price = price;
		}
		public int getColorId() {
			return colorId;
		}
		public void setColorId(int colorId) {
			this.colorId = colorId;
		}
		public String getProductName() {
			return productName;
		}
		public void setProductName(String productName) {
			this.productName = productName;
		}
		public int getProductId() {
			return productId;
		}
		public void setProductId(int productId) {
			this.productId = productId;
		}
	}
	
	public GiftsDP(int numOfGifts, float budget) {
		this.numOfGifts = numOfGifts;
		this.budget = budget;
		this.diffThreshold = (float) ((budget/numOfGifts) * 0.01);
		this.retryAttempts = 3;
	}
		
	public void apiCall(int page) throws JSONException, IOException {
		int limit = 100;
		JSONArray excludes = new JSONArray();
		excludes.put("styleId");
		excludes.put("brandName");
		excludes.put("thumbnailImageUrl");
		excludes.put("productUrl");
		excludes.put("originalPrice");
		excludes.put("percentOff");
		String options = "sort={\"productPopularity\":\"desc\"}&limit="+limit+
				"&excludes="+excludes+"&page="+page;
		String url = BASE_URL + "Search/?" + options + "&key=" + KEY;
		URLConnection conn = new URL(url).openConnection();
		InputStream response;
		try {
			response = conn.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			retryAttempts--;
			if(retryAttempts > 0)
			{
				System.out.println("\nRetrying ...\n\n");
				apiCall(1);
			}
			else
				System.out.println("\nExiting ...\n");
			return;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(response));
		String json = "",line;
		while((line = br.readLine())!=null)
				json += line;
		JSONArray ja = (JSONArray) (new JSONObject(json)).get("results");
		Product p;
		for(int i=0;i<ja.length();i++)
		{
			p = new Product(ja.get(i).toString());
			if(p.getPrice() <= budget)
				inputProductsList.add(p);
		}
		while(inputProductsList.isEmpty())
			apiCall(page+1);
		processResults();
	}
	
	public void processResults() {
		pickGifts(0, inputProductsList.size(), budget, numOfGifts);
		Collections.sort(outputProductsList, new Comparator<List<Product>>() {

			@Override
			public int compare(List<Product> o1, List<Product> o2) {
				float totalPrice1=0, totalPrice2=0;
				for(Product p : o1) {
					totalPrice1 += p.getPrice();
				}
				for(Product p : o2) {
					totalPrice2 += p.getPrice();
				}
				return (int)(totalPrice2 - totalPrice1);
			}
		});
		for(List<Product> products : outputProductsList) {
			System.out.println("\n\n"+products);
		}
		System.out.println("Count: "+outputProductsList.size());
	}
	
	private void pickGifts(int start, int end, float tmpBudget,
			int num) {
		int key = getHashCode(tmpBudget, num);
		if(this.lowestNoMatchFoundIndex.containsKey(key))
		{
			if(this.lowestNoMatchFoundIndex.get(key) <= start)
				return;
		}
		Product item;
		float price;
		int resultCountBefore = resultCount;
		for(int i=start;i<end;i++)
		{
			item = inputProductsList.get(i);
			price = item.getPrice();
			if(price <= tmpBudget)
			{
				if(num == 1)
				{
					double diff = tmpBudget - price;
					if(diff <= this.diffThreshold)
					{
						pickList.add(item);
						ArrayList<Product> list = new ArrayList<Product>();
						list.addAll(pickList);
						resultCount++;
						outputProductsList.add(list);
						pickList.remove(pickList.size()-1);
					}
				}
				else
				{
					pickList.add(item);
					pickGifts(i+1, end, tmpBudget-price, num-1);
					pickList.remove(pickList.size()-1);
				}
			}
		}
		if(this.resultCount == resultCountBefore)
		{
			this.lowestNoMatchFoundIndex.put(key, start);
		}	
	}

	private int getHashCode(float tmpBudget, int num) {
		int hashCode = (int) (17 * 37 + tmpBudget);
		hashCode = hashCode * 37 + num;
		return hashCode;
	}

	public static void main(String[] args) throws IOException, JSONException {
		Scanner sc = new Scanner(System.in);
		System.out.print("\nEnter number of gifts: ");
		int giftNum = sc.nextInt();
		System.out.print("\nEnter budget in USD: ");
		float budget = sc.nextFloat();
		GiftsDP g = new GiftsDP(giftNum, budget);
		g.apiCall(1); 
	}

}
