package com.lzw.talk.view.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.*;
import com.lzw.talk.R;
import com.lzw.talk.base.App;
import com.lzw.talk.util.Logger;
import com.lzw.talk.util.Utils;
import com.lzw.talk.view.HeaderLayout;

/**
 * ���ڷ���λ�õĽ���
 *
 * @author smile
 * @ClassName: LocationActivity
 * @Description: TODO
 * @date 2014-6-23 ����3:17:05
 */
public class LocationActivity extends BaseActivity implements
    OnGetGeoCoderResultListener {

  // ��λ���
  LocationClient mLocClient;
  public MyLocationListenner myListener = new MyLocationListenner();
  BitmapDescriptor mCurrentMarker;

  MapView mMapView;
  BaiduMap mBaiduMap;
  HeaderLayout headerLayout;

  private BaiduReceiver mReceiver;// ע��㲥�����������ڼ��������Լ���֤key

  GeoCoder mSearch = null; // ����ģ�飬��Ϊ�ٶȶ�λsdk�ܹ��õ���γ�ȣ�����ȴ�޷��õ��������ϸ��ַ�������Ҫ��ȡ�����뷽ʽȥ�����˾�γ�ȴ����ĵ�ַ

  static BDLocation lastLocation = null;

  BitmapDescriptor bdgeo = BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_location);
    initBaiduMap();
  }

  private void initBaiduMap() {
    // ��ͼ��ʼ��
    mMapView = (MapView) findViewById(R.id.bmapView);
    headerLayout = (HeaderLayout) findViewById(R.id.headerLayout);
    mBaiduMap = mMapView.getMap();
    //�������ż���
    mBaiduMap.setMaxAndMinZoomLevel(18, 13);
    // ע�� SDK �㲥������
    IntentFilter iFilter = new IntentFilter();
    iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
    iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
    mReceiver = new BaiduReceiver();
    registerReceiver(mReceiver, iFilter);

    Intent intent = getIntent();
    String type = intent.getStringExtra("type");
    if (type.equals("select")) {// ѡ����λ��
      headerLayout.showTitle(R.string.position);
      headerLayout.showRightImageButton(R.drawable.btn_login_selector, new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          gotoChatPage();
        }
      });
      initLocClient();
    } else {// �鿴��ǰλ��
      headerLayout.showTitle(R.string.position);
      Bundle b = intent.getExtras();
      LatLng latlng = new LatLng(b.getDouble("latitude"), b.getDouble("longtitude"));//ά����ǰ�������ں�
      mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(latlng));
      //��ʾ��ǰλ��ͼ��
      OverlayOptions ooA = new MarkerOptions().position(latlng).icon(bdgeo).zIndex(9);
      mBaiduMap.addOverlay(ooA);
    }

    mSearch = GeoCoder.newInstance();
    mSearch.setOnGetGeoCodeResultListener(this);

  }

  /**
   * �ص��������
   *
   * @param
   * @return void
   * @throws
   * @Title: gotoChatPage
   * @Description: TODO
   */
  private void gotoChatPage() {
    if (lastLocation != null) {
      Intent intent = new Intent();
      intent.putExtra("y", lastLocation.getLongitude());// ����
      intent.putExtra("x", lastLocation.getLatitude());// ά��
      intent.putExtra("address", lastLocation.getAddrStr());
      setResult(RESULT_OK, intent);
      this.finish();
    } else {
      Utils.toast(App.ctx, R.string.getGeoInfoFailed);
    }
  }

  private void initLocClient() {
//		 ������λͼ��
    mBaiduMap.setMyLocationEnabled(true);
    mBaiduMap.setMyLocationConfigeration(new MyLocationConfigeration(
        MyLocationConfigeration.LocationMode.NORMAL, true, null));
    // ��λ��ʼ��
    mLocClient = new LocationClient(this);
    mLocClient.registerLocationListener(myListener);
    LocationClientOption option = new LocationClientOption();
    option.setProdName("bmobim");// ���ò�Ʒ��
    option.setOpenGps(true);// ��gps
    option.setCoorType("bd09ll"); // ������������
    option.setScanSpan(1000);
    option.setOpenGps(true);
    option.setIsNeedAddress(true);
    option.setIgnoreKillProcess(true);
    mLocClient.setLocOption(option);
    mLocClient.start();
    if (mLocClient != null && mLocClient.isStarted())
      mLocClient.requestLocation();

    if (lastLocation != null) {
      // ��ʾ�ڵ�ͼ��
      LatLng ll = new LatLng(lastLocation.getLatitude(),
          lastLocation.getLongitude());
      MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
      mBaiduMap.animateMapStatus(u);
    }
  }

  /**
   * ��λSDK��������
   */
  public class MyLocationListenner implements BDLocationListener {

    @Override
    public void onReceiveLocation(BDLocation location) {
      // map view ���ٺ��ڴ����½��յ�λ��
      if (location == null || mMapView == null)
        return;

      if (lastLocation != null) {
        if (lastLocation.getLatitude() == location.getLatitude()
            && lastLocation.getLongitude() == location
            .getLongitude()) {
          Logger.d(App.ctx.getString(R.string.geoIsSame));// �����������ȡ���ĵ���λ����������ͬ�ģ����ٶ�λ
          mLocClient.stop();
          return;
        }
      }
      lastLocation = location;

      Logger.d("lontitude = " + location.getLongitude() + ",latitude = "
          + location.getLatitude() + "," + App.ctx.getString(R.string.position) + " = "
          + lastLocation.getAddrStr());

      MyLocationData locData = new MyLocationData.Builder()
          .accuracy(location.getRadius())
              // �˴����ÿ����߻�ȡ���ķ�����Ϣ��˳ʱ��0-360
          .direction(100).latitude(location.getLatitude())
          .longitude(location.getLongitude()).build();
      mBaiduMap.setMyLocationData(locData);
      LatLng ll = new LatLng(location.getLatitude(),
          location.getLongitude());
      String address = location.getAddrStr();
      if (address != null && !address.equals("")) {
        lastLocation.setAddrStr(address);
      } else {
        // ��Geo����
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ll));
      }
      // ��ʾ�ڵ�ͼ��
      MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
      mBaiduMap.animateMapStatus(u);
      //���ð�ť�ɵ��
    }
  }

  /**
   * ����㲥�����࣬���� SDK key ��֤�Լ������쳣�㲥
   */
  public class BaiduReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
      String s = intent.getAction();
      if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
        Utils.toast(ctx, App.ctx.getString(R.string.mapKeyErrorTips));
      } else if (s
          .equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
        Utils.toast(ctx, App.ctx.getString(R.string.badNetwork));
      }
    }
  }

  @Override
  public void onGetGeoCodeResult(GeoCodeResult arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
    // TODO Auto-generated method stub
    if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
      Utils.toast(ctx, App.ctx.getString(R.string.cannotFindResult));
      return;
    }
    Logger.d(App.ctx.getString(R.string.reverseGeoCodeResultIs) + result.getAddress());
    lastLocation.setAddrStr(result.getAddress());
  }

  @Override
  protected void onPause() {
    mMapView.onPause();
    super.onPause();
    lastLocation = null;
  }

  @Override
  protected void onResume() {
    mMapView.onResume();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    if (mLocClient != null && mLocClient.isStarted()) {
      // �˳�ʱ���ٶ�λ
      mLocClient.stop();
    }
    // �رն�λͼ��
    mBaiduMap.setMyLocationEnabled(false);
    mMapView.onDestroy();
    mMapView = null;
    // ȡ������ SDK �㲥
    unregisterReceiver(mReceiver);
    super.onDestroy();
    // ���� bitmap ��Դ
    bdgeo.recycle();
  }

}