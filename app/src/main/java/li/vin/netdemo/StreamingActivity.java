package li.vin.netdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;
import li.vin.net.Coordinate;
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
    private MarkerOptions deviceMarkerOptions;
    private Marker deviceMarker;

    @Bind(R.id.rpm) TextView rpmTextView;
    @Bind(R.id.vehicle_speed) TextView vehicleSpeedTextView;
    @Bind(R.id.mass_air_flow)TextView massAirFlowTextView;
    @Bind(R.id.calculated_load_value) TextView calculatedLoadValueTextView;
    @Bind(R.id.intake_manifold_pressure) TextView intakeManifoldPressureTextView;
    @Bind(R.id.engine_coolant_temp) TextView engineCoolantTempTextView;
    @Bind(R.id.throttle_position) TextView throttlePositionTextView;
    @Bind(R.id.time_since_engine_start) TextView timeSinceEngineStartTextView;
    @Bind(R.id.fuel_rail_pressure) TextView fuelRailPressureTextView;
    @Bind(R.id.fuel_pressure) TextView fuelPressureTextView;
    @Bind(R.id.intake_air_temperature) TextView intakeAirTemperatureTextView;
    @Bind(R.id.timing_advance) TextView timingAdvanceTextView;

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

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(DataType.INTAKE_MANIFOLD_PRESSURE))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        intakeManifoldPressureTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.intake_manifold_pressure)));
                    }

                    @Override
                    public void onNext(Integer pressure) {
                        Log.e("TESTO", "stream intake manifold pressure for " + device.name() + " : " + pressure);
                        intakeManifoldPressureTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.intake_manifold_pressure), pressure));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(DataType.ENGINE_COOLANT_TEMP))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        engineCoolantTempTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.coolant_temp)));
                    }

                    @Override
                    public void onNext(Integer temperature) {
                        Log.e("TESTO", "stream coolant temperature for " + device.name() + " : " + temperature);
                        engineCoolantTempTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.coolant_temp), temperature));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithDoubleVal(DataType.THROTTLE_POSITION))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Double>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        throttlePositionTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.throttle_position)));
                    }

                    @Override
                    public void onNext(Double position) {
                        Log.e("TESTO", "stream throttle position for " + device.name() + " : " + position);
                        throttlePositionTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.throttle_position), position));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(DataType.TIME_SINCE_ENGINE_START))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        timeSinceEngineStartTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.time_since_engine_start)));
                    }

                    @Override
                    public void onNext(Integer time) {
                        Log.e("TESTO", "stream time running for " + device.name() + " : " + time);
                        timeSinceEngineStartTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.time_since_engine_start), time));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithDoubleVal(DataType.FUEL_RAIL_PRESSURE))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Double>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        fuelRailPressureTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.fuel_rail_pressure)));
                    }

                    @Override
                    public void onNext(Double pressure) {
                        Log.e("TESTO", "stream fuel rail pressure for " + device.name() + " : " + pressure);
                        fuelRailPressureTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.fuel_rail_pressure), pressure));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(DataType.FUEL_PRESSURE))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        fuelPressureTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.fuel_pressure)));
                    }

                    @Override
                    public void onNext(Integer pressure) {
                        Log.e("TESTO", "stream fuel pressure for " + device.name() + " : " + pressure);
                        fuelPressureTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.fuel_pressure), pressure));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithIntVal(DataType.INTAKE_AIR_TEMP))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        intakeAirTemperatureTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.intake_air_temp)));
                    }

                    @Override
                    public void onNext(Integer temperature) {
                        Log.e("TESTO", "stream intake air temperature for " + device.name() + " : " + temperature);
                        intakeAirTemperatureTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.intake_air_temp), temperature));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.onlyWithDoubleVal(DataType.TIMING_ADVANCE))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Double>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        timingAdvanceTextView.setText(String.format(getString(R.string.error_fmt), getString(R.string.timing_advance)));
                    }

                    @Override
                    public void onNext(Double degrees) {
                        Log.e("TESTO", "stream timing advance for " + device.name() + " : " + degrees);
                        timingAdvanceTextView.setText(String.format(getString(R.string.success_fmt), getString(R.string.timing_advance), degrees));
                    }
                }));

        subscription.add(stream
                .flatMap(StreamMessage.coordinate())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Coordinate>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Coordinate coordinate) {
                        Log.e("TESTO", "stream coord for " + device.name() + " : " + coordinate);
                        updateMap(coordinate);
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

    private void updateMap(Coordinate coordinate){
        LatLng latLng = new LatLng(coordinate.lat(), coordinate.lon());

        if(deviceMarker == null){
            deviceMarkerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(device.name());

            deviceMarker = googleMap.addMarker(deviceMarkerOptions);
        }else{
            deviceMarker.setPosition(latLng);
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f));
    }
}
