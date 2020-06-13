package me.noodian.corona.ui;

public interface Displayable {
	void addSubscriber(UiDisplay subscriber);
	void removeSubscriber(UiDisplay subscriber);
}