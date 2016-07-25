package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.AbstractMap;

import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private  void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
        
        List<Entry<String, Integer>> list1 = sort();
        List<Entry<String, Integer>> list2 = that.sort();
        
        Map<String, Integer> unionMap = new HashMap<String, Integer>();
        
        for(int i = 0; i< list1.size(); i++){
        	for(int j = 0; j< list2.size(); j++){
        		
        		if(list1.get(i).getKey().equals(list2.get(j).getKey())){
        			unionMap.put(list1.get(i).getKey(), totalRelevance(list1.get(i).getValue(), list2.get(j).getValue()));
        			break;
        		}else if(!unionMap.containsKey(list1.get(i).getKey()) || !unionMap.containsKey(list2.get(j).getKey())){
        			unionMap.put(list1.get(i).getKey(), list1.get(i).getValue());
        			unionMap.put(list2.get(j).getKey(), list2.get(j).getValue());
        		}
        	}
        	
        }
        
        WikiSearch union = new WikiSearch(unionMap);
		return union;
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        
        List<Entry<String, Integer>> list1 = sort();
        List<Entry<String, Integer>> list2 = that.sort();
        Map<String, Integer> intersectionMap = new HashMap<String, Integer>();
        
        
        for(int i = 0; i< list1.size(); i++){
        	for(int j = 0; j< list2.size(); j++){
        		if(list1.get(i).getKey().equals(list2.get(j).getKey())){
        			intersectionMap.put(list1.get(i).getKey(), 
        				totalRelevance(list1.get(i).getValue(), list2.get(j).getValue()));
        		}
        	}
        	
        }
        
        WikiSearch intersection = new WikiSearch(intersectionMap);
		return intersection;
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
        
		List<Entry<String, Integer>> list1 = sort();
        List<Entry<String, Integer>> list2 = that.sort();
        Map<String, Integer> differenceMap = new HashMap<String, Integer>();
        
        boolean contains = false;
        
        
        for(int i = 0; i< list1.size(); i++){
        	for(int j = 0; j< list2.size(); j++){
        		if(list1.get(i).getKey().equals(list2.get(j).getKey())){
        			contains = true;
        			break;
        		}
        	}
        	if(contains == false){
        		differenceMap.put(list1.get(i).getKey(), list1.get(i).getValue());
        	}
        	
        }
        
        WikiSearch difference = new WikiSearch(differenceMap);
		return difference;
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
       
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(map.entrySet());
        
        List<Term> termList = makeComparable(list);
        
        Collections.sort(termList);
        
        Map<String, Integer> termMap = new HashMap<String, Integer>();
        
        List<Entry<String, Integer>> newlist = new ArrayList<Entry<String, Integer>>();
        
        for(Term term: termList){
        	
        	Map.Entry<String,Integer> entry = 
        		new AbstractMap.SimpleEntry<String, Integer>(term.getURL(), term.getFrequency());
        	
        	newlist.add(entry);
        }
        
        return newlist;
	}

	public static List<Term> makeComparable(List<Entry<String, Integer>> list) {
        
        List<Term> terms = new ArrayList<Term>();
        
        for(Entry<String, Integer> entry: list){
        	
        	Term term = new Term(entry.getKey(), entry.getValue());
        	terms.add(term);
        
        }
        
        return terms;
    
    }

	public static class Term implements Comparable<Term> {
 
		private final int frequency;
		private final String url;

		public Term(String url, int frequency) {
			this.url = url;
		    this.frequency = frequency;
		}

		public int getFrequency(){
			return frequency;
		}

		public String getURL(){
			return url;
		}
		@Override
		public int compareTo(Term that){
			
			if (this.frequency < that.frequency) {
            return -1;
		    }
		    if (this.frequency > that.frequency) {
		        return 1;
		    }
		    
		    return 0;
		}

    }

	

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

	public static void main(String[] args) throws IOException {
		
		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		
		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		search1.print();
		
		// search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();
		
		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();
	}
}
