package li.vin.netdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import li.vin.net.Device;
import li.vin.net.StreamMessage;
import li.vin.net.Vinli;
import li.vin.net.VinliApp;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;

public class StreamingActivity extends AppCompatActivity implements OnMapReadyCallback{

    private CompositeSubscription subscription;
    private VinliApp vinliApp;
    private Device device;
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        vinliApp = Vinli.loadApp(this);

        Intent intent = getIntent();
        this.device = intent.getParcelableExtra(getString(R.string.streaming_device_key));

        setTitle(device.name());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        subscribeAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cleanupSubscription();
    }

    private void subscribeAll() {
        if (vinliApp == null) return;

        cleanupSubscription();

        subscription = new CompositeSubscription();

        ConnectableObservable<StreamMessage> stream = device.stream().publish();

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(StreamMessage.DataType.RPM))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer rpm) {
                        Log.e("TESTO", "stream RPM for " + device.name() + " : " + rpm);
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(StreamMessage.DataType.VEHICLE_SPEED))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer vss) {
                        Log.e("TESTO", "stream VSS for " + device.name() + " : " + vss);
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithFloatVal(StreamMessage.DataType.MASS_AIRFLOW))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Float>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Float maf) {
                        Log.e("TESTO", "stream MAF for " + device.name() + " : " + maf);
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithFloatVal(StreamMessage.DataType.CALCULATED_ENGINE_LOAD))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Float>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Float load) {
                        Log.e("TESTO", "stream load% for " + device.name() + " : " + load);
                    }
                }));

        stream.connect();

    }

    /** Unsubscribe all. Need to call this to clean up rx resources. */
    private void cleanupSubscription() {
        if (subscription != null) {
            if (!subscription.isUnsubscribed()) subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        this.googleMap = gMap;
    }
}
