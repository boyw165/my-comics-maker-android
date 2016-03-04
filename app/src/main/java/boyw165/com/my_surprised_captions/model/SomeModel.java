package boyw165.com.my_surprised_captions.model;

import rx.Observable;
import rx.subjects.PublishSubject;

public class SomeModel {

    private PublishSubject<Object> mBus = PublishSubject.create();

    public SomeModel() {
    }

    public Observable<Object> getObservable() {
        return null;
    }
}
