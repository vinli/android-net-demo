package li.vin.netdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import butterknife.Bind;
import butterknife.ButterKnife;
import li.vin.net.Device;
import li.vin.net.StreamMessage;
import li.vin.net.StreamMessage.DataType;
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

    @Bind(R.id.rpm) TextView rpmTextView;
    @Bind(R.id.vehicle_speed) TextView vehicleSpeedTextView;
    @Bind(R.id.mass_air_flow)TextView massAirFlowTextView;
    @Bind(R.id.calculated_load_value) TextView calculatedLoadValueTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

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
                .flatMap(StreamMessage.onlyWithIntVal(DataType.RPM))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        rpmTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.rpm)));
                    }

                    @Override
                    public void onNext(Integer rpm) {
                        Log.e("TESTO", "stream RPM for " + device.name() + " : " + rpm);
                        rpmTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.rpm), rpm));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(DataType.VEHICLE_SPEED))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        vehicleSpeedTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.vehicle_speed)));
                    }

                    @Override
                    public void onNext(Integer vss) {
                        Log.e("TESTO", "stream VSS for " + device.name() + " : " + vss);
                        vehicleSpeedTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.vehicle_speed), vss));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithFloatVal(DataType.MASS_AIRFLOW))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Float>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        massAirFlowTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.mass_air_flow)));
                    }

                    @Override
                    public void onNext(Float maf) {
                        Log.e("TESTO", "stream MAF for " + device.name() + " : " + maf);
                        massAirFlowTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.mass_air_flow), maf));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithFloatVal(DataType.CALCULATED_ENGINE_LOAD))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Float>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        calculatedLoadValueTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.calculated_load_value)));
                    }

                    @Override
                    public void onNext(Float load) {
                        Log.e("TESTO", "stream load% for " + device.name() + " : " + load);
                        calculatedLoadValueTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.calculated_load_value), load));
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
