package com.flaredown.flaredownApp.Helpers.Observers;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages an observable allowing for multiple subscribers to be notified with one method call.
 */
public class ObservableHelper<T>{
    private Observable<T> observable;
    private List<Subscriber<? super T>> subscriberList = new ArrayList<Subscriber<? super T>>();

    public ObservableHelper() {
        observable = Observable.create(new Observable.OnSubscribe<T>() {
            public void call(Subscriber<? super T> subscriber) {
                subscriberList.add(subscriber);
            }
        });
    }

    /**
     * Notify all subscribers of update
     * @param object
     */
    public void notifySubscribers(T object) {
        for (int i = 0; i < subscriberList.size(); i++) {
            Subscriber<? super T> subscriber = subscriberList.get(i);
            if(subscriber.isUnsubscribed()) {
                subscriberList.remove(subscriber);
                i--;
            } else {
                subscriber.onNext(object);
            }
        }
    }

    /**
     * Add subscriber to the observable.
     * @param subscriber The subscriber to observer the observable.
     */
    public Subscription subscribe(Subscriber<T> subscriber) {
        return observable.subscribe(subscriber);
    }

    /**
     * Add action subscriber to the observable.
     * @param action1 The action which observes the observable.
     */
    public Subscription subscribe(Action1<T> action1) {
        return observable.subscribe(action1);
    }
}