package me.noodian.corona.ui;

public interface Displayable {
	void onSubscriberAdded(Display subscriber);
	void onSubscriberRemoved(Display subscriber);
}