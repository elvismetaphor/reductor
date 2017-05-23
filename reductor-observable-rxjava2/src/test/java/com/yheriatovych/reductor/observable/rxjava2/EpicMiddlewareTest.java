package com.yheriatovych.reductor.observable.rxjava2;

import com.yheriatovych.reductor.Action;
import com.yheriatovych.reductor.Reducer;
import com.yheriatovych.reductor.Store;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class EpicMiddlewareTest {

    private class TestState {

    }

    private class TestReducer implements Reducer<TestState> {
        @Override
        public TestState reduce(TestState testState, Action action) {
            if(testState == null) {
                testState = new TestState();
            }
            return testState;
        }
    }

    @Spy TestReducer reducer = new TestReducer();
    PublishSubject<Object> epicObservable = PublishSubject.create();
    @Mock
    Epic<TestState> epic;
    @Captor ArgumentCaptor<Observable<Action>> actionsCaptor;
    Store<TestState> store;
    EpicMiddleware<TestState> epicMiddleware;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(epic.run(actionsCaptor.capture(), any())).thenReturn(epicObservable);
        epicMiddleware = EpicMiddleware.create(epic);
    }

    @Test
    public void testSubscribeToEpic() {
        store = Store.create(reducer, epicMiddleware);

        verify(epic).run(any(), eq(store));
        assertTrue(epicObservable.hasObservers());
    }

    @Test
    public void testPropagateActionsToEpic() {
        store = Store.create(reducer, epicMiddleware);

        TestObserver<Action> testObserver = TestObserver.create();
        actionsCaptor.getValue().subscribe(testObserver);

        Action testAction = Action.create("TEST");
        store.dispatch(testAction);

        testObserver.assertValue(testAction);
    }

    @Test
    public void testUnsubscriptionEpic() {
        store = Store.create(reducer, epicMiddleware);

        assertTrue("epic observable should has observers after Store.create", epicObservable.hasObservers());

        assertFalse("Epic should not be unsubscribed", epicMiddleware.isDisposed());
        epicMiddleware.dispose();
        assertTrue("Epic should be unsubscribed", epicMiddleware.isDisposed());

        assertFalse("epic observable is unsubscibed after middleware.unsubscribe", epicObservable.hasObservers());
    }
}