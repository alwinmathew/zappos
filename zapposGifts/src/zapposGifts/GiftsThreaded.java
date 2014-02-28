/*
 * This is the second version of the program where I have used
 * multi-threading to find the products list.
 * 
 * I spawn multiple threads to compute the results in parallel.
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
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GiftsThreaded {

	private static final String KEY = "52ddafbe3ee659bad97fcce7c53592916a6bfd73";
	private static final String BASE_URL = "http://api.zappos.com/";
	private int giftNum;
	private float budget;
	private ArrayList<Product> productList = new ArrayList<GiftsThreaded.Product>();
	
	public static class GiftProcessor implements Runnable {

		private ArrayList<Product> itemList;
		private int start;
		private int end;
		private float budget;
		private int giftNum;
		private ArrayList<Product> pickList = new ArrayList<Product>();
		private static int count = 0;
		public GiftProcessor(ArrayList<Product> productList, int start, int end, float budget, int giftNum) {
			this.itemList = productList;
			this.start = start;
			if(end > productList.size())
				this.end = productList.size();
			else
				this.end = end;
			this.budget = budget;
			this.giftNum = giftNum;
		}
		
		@Override
		public void run() {
			try {
				pickGifts(start, end, budget, giftNum);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		public static int getResultCount() {
			return count;
		}
		
		private void pickGifts(int start, int end, float tmpBudget,
				int num) throws JSONException {
			Product p;
			float price;
			for(int i=start;i<end;i++)
			{
				p = itemList.get(i);
				price = p.getPrice();
				if(price <= tmpBudget)
				{
					if(num == 1)
					{
						float diff = tmpBudget - price;
						if(diff <= (this.budget/this.giftNum)*0.01)
						{
							pickList.add(p);
							synchronized (this) {
								System.out.println(pickList);
								count++;
							}
							pickList.remove(pickList.size()-1);
						}
					}
					else
					{
						pickList.add(p);
						pickGifts(i+1, itemList.size(), tmpBudget-price, num-1);
						pickList.remove(pickList.size()-1);
					}
				}
			}	
		}	
	}
	
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
	}
	
	public GiftsThreaded(int giftNum, float budget) {
		this.giftNum = giftNum;
		this.budget = budget;
	}
		
	public void apiCall(int page) throws IOException, JSONException {
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
		InputStream response = conn.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(response));
		String json = "",line;
		while((line = br.readLine())!=null)
				json += line;
		JSONArray ja = (JSONArray) (new JSONObject(json)).get("results");
		Product p;
		for(int i=0;i<ja.length();i++)
		{
			p = new Product(ja.get(i).toString());
			if(p.getPrice() < this.budget)
				productList.add(p);
		}
		while(productList.isEmpty())
			apiCall(page+1);
	}
	
	public void processResults() throws InterruptedException, JSONException {
		
		int end = productList.size();
		int threadCount = giftNum;
		ArrayList<Integer> steps = new ArrayList<Integer>();
		int tmp = end;
		while(threadCount>1) {
			tmp/=2;
			steps.add(tmp);
			threadCount--;
		}
		GiftProcessor gp;
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for(int i=0;i<=(end-giftNum);)
		{
			if(steps.isEmpty())
				tmp = end;
			else
				tmp = steps.remove(steps.size()-1);
			gp = new GiftProcessor(productList, i, tmp, this.budget, this.giftNum);
			Thread t = new Thread(gp);
			threads.add(t);
			t.start();
			i = tmp;
		}
		for(Thread t: threads)
			t.join();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, JSONException {
		Scanner sc = new Scanner(System.in);
		System.out.print("\nEnter number of gifts: ");
		int giftNum = sc.nextInt();
		System.out.print("\nEnter budget in USD: ");
		float budget = sc.nextFloat();
		GiftsThreaded g = new GiftsThreaded(giftNum, budget);
		int page = 1;
		while(GiftProcessor.getResultCount() == 0 && page <= 3)
		{
			g.apiCall(page++);
			g.processResults();
		}
		 
	}

}
