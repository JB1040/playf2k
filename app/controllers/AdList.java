package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Advertisement;

public class AdList {
	protected List<Advertisement> ads = new ArrayList<>();
	
	public AdList() {
		
	}

	public void setAds(List<Advertisement>  ads) {
		this.ads = ads;
	}

	public List<Advertisement> getAds() {
		return this.ads;
	}
}

