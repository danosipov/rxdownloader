package com.danosipov.asyncandroid.rxdownloader;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.danosipov.asyncandroid.rxdownloader.events.ClickEvent;
import com.danosipov.asyncandroid.rxdownloader.events.DownloadPauseEvent;
import com.danosipov.asyncandroid.rxdownloader.events.DownloadProgressEvent;
import com.danosipov.asyncandroid.rxdownloader.events.DownloadResumeEvent;
import com.danosipov.asyncandroid.rxdownloader.events.DownloadStartEvent;
import com.danosipov.asyncandroid.rxdownloader.events.ResetEvent;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.android.observables.AndroidObservable;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

public class DownloadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new DownloadFragment())
                    .commit();
        }

    }

    public static class DownloadFragment extends Fragment implements Observer<ClickEvent> {

        private ProgressBar progressBar;
        private EditText urlEditText;
        private Downloader downloadThread;
        private TextView downloadProgress;

        private View.OnClickListener handleReset;
        private View.OnClickListener handleDownload;
        private View.OnClickListener handlePause;
        private View.OnClickListener handleResume;

        private Subscription clickSubscription;

        public DownloadFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_download, container, false);
            setRetainInstance(true);

            progressBar = ((ProgressBar) rootView.findViewById(R.id.downloadProgressBar));
            urlEditText = ((EditText) rootView.findViewById(R.id.urlEditText));
            downloadProgress = ((TextView) rootView.findViewById(R.id.downloadProgressTextView));

            // See how to better handle this code:
            Observable<ClickEvent> clickObservable = Observable.create(
                    new Observable.OnSubscribeFunc<ClickEvent>() {
                        @Override
                        public Subscription onSubscribe(final Observer<? super ClickEvent> observer) {
                            handleReset = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    observer.onNext(new ResetEvent(v));
                                }
                            };
                            handleDownload = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    observer.onNext(new DownloadStartEvent(v));
                                }
                            };
                            handlePause = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    observer.onNext(new DownloadPauseEvent(v));
                                }
                            };
                            handleResume = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    observer.onNext(new DownloadResumeEvent(v));
                                }
                            };

                            Button resetButton = ((Button) rootView.findViewById(R.id.resetButton));
                            resetButton.setOnClickListener(handleReset);
                            Button downloadButton = ((Button) rootView.findViewById(R.id.downloadButton));
                            downloadButton.setOnClickListener(handleDownload);
                            /**
                             * Restore state of the views based on the fragment instance state
                             * If not done, the center button stays in "download" state that
                             * the view is initialized with
                             */
                            if (downloadThread != null) {
                                if (downloadThread.isRunning() && !downloadThread.isKilled()) {
                                    switchToPause(downloadButton);
                                } else if (!downloadThread.isRunning() && !downloadThread.isKilled()) {
                                    switchToResume(downloadButton);
                                }
                            }

                            return Subscriptions.empty();
                        }
                    });

            clickSubscription = AndroidObservable.fromFragment(this, clickObservable)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(this);

            return rootView;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onDestroy() {
            clickSubscription.unsubscribe();
            super.onDestroy();
        }

        public void answerDownloadStart(DownloadStartEvent event) {
            downloadThread = new Downloader(urlEditText.getText().toString());
            downloadThread.start();
            downloadThread.getProgressObservable()
                    .sample(30, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<DownloadProgressEvent>() {
                        @Override
                        public void call(DownloadProgressEvent event) {
                            progressBar.setProgress((int) event.getProgress());
                            downloadProgress.setText(String.format("%s / %s",
                                    event.getLoadedBytes(), event.getTotalBytes()));
                        }
                    });
            switchToPause(((Button) event.getView()));
        }

        public void answerDownloadPause(DownloadPauseEvent event) {
            downloadThread.pause(true);
            switchToResume(((Button) event.getView()));
        }

        public void answerDownloadResume(DownloadResumeEvent event) {
            downloadThread.pause(false);
            switchToPause(((Button) event.getView()));
        }

        public void answerReset(ResetEvent event) {
            if (downloadThread != null && downloadThread.isAlive()) {
                downloadThread.kill();
            }
            switchToDownload(((Button) getView().findViewById(R.id.downloadButton)));
        }

        private void switchToPause(Button downloadButton) {
            downloadButton.setText(getString(R.string.pause));
            downloadButton.setOnClickListener(handlePause);
        }

        private void switchToResume(Button downloadButton) {
            downloadButton.setText(getString(R.string.resume));
            downloadButton.setOnClickListener(handleResume);
        }

        private void switchToDownload(Button downloadButton) {
            downloadButton.setText(getString(R.string.download));
            downloadButton.setOnClickListener(handleDownload);
            downloadProgress.setText(getString(R.string.zero_bytes));
            progressBar.setProgress(0);
        }

        @Override
        public void onCompleted() {
            // ignore
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(DownloadActivity.class.toString(), "Got an error from the Observable", throwable);
        }

        @Override
        public void onNext(ClickEvent clickEvent) {
            if (clickEvent instanceof ResetEvent) {
                answerReset((ResetEvent) clickEvent);
            }
            if (clickEvent instanceof DownloadResumeEvent) {
                answerDownloadResume((DownloadResumeEvent) clickEvent);
            }
            if (clickEvent instanceof DownloadPauseEvent) {
                answerDownloadPause((DownloadPauseEvent) clickEvent);
            }
            if (clickEvent instanceof DownloadStartEvent) {
                answerDownloadStart((DownloadStartEvent) clickEvent);
            }
        }
    }
}
