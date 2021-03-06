package com.example.dam.uebung2;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Method;

public class MainCellAsyncTask extends AsyncTask<String, Void, String> {
    TelephonyManager telephonyManager;
    MyPhoneStateListener MyListener;
    Activity activity;
    int signalLevel;
    int signalStrengthASU;
    int signalStrengthDBM;


    public MainCellAsyncTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... params) {
        // infinite LOOP
        while (true) {
            try {

                if (CellInfoUtils.getRefreshRateInSec() != -1) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshMainCell();
                        }
                    });

                    Thread.sleep(CellInfoUtils.getRefreshRateInSec() * 1000);
                }

            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
       // return "Executed";
    }

    @Override
    protected void onPreExecute() {
        telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        MyListener = new MyPhoneStateListener();
        telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        /* Get the Signal strength from the provider, each time there is an update */
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            System.out.println(signalStrength);

            if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
                try {
                    Class cSignalStrength = Class.forName("android.telephony.SignalStrength");
                    Method mGetLteAsuLevel = cSignalStrength.getMethod("getLteAsuLevel");
                    Method mGetLteDbm = cSignalStrength.getMethod("getLteDbm");
                    signalStrengthASU = (int) mGetLteAsuLevel.invoke(signalStrength);
                    signalStrengthDBM = (int) mGetLteDbm.invoke(signalStrength);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                signalStrengthASU = signalStrength.getGsmSignalStrength();
                signalStrengthDBM = CellInfoUtils.ASUToRSSI(signalStrengthASU, telephonyManager.getNetworkType());

                if (signalStrengthASU == 0) {
                    signalStrengthDBM = new Integer(signalStrength.toString().split(" ")[3]);
                    signalStrengthASU = CellInfoUtils.RSSIToASU(signalStrengthDBM, telephonyManager.getNetworkType());
                }
                // swap ASU with DBM
                else if (signalStrengthASU < 0) {
                    signalStrengthDBM = signalStrengthASU;
                    signalStrengthASU = CellInfoUtils.RSSIToASU(signalStrengthDBM, telephonyManager.getNetworkType());
                }
            }

            signalLevel = CellInfoUtils.getSignalLevelFromRSSI(signalStrengthDBM);
            // disconnected, no signal
            if (signalStrengthASU == 0) {
                signalLevel = -1;
            }
        }

    }

    public void refreshMainCell() {
        GsmCellLocation cellLocation = (GsmCellLocation) telephonyManager.getCellLocation();

        int cid = -1, lac = -1, psc = -1;
        if (cellLocation != null) {
            // cell id
            cid = cellLocation.getCid();
            // location area code
            lac = cellLocation.getLac();
            psc = cellLocation.getPsc();
        }
        String networkOperator = telephonyManager.getNetworkOperator();
        int mccNo = -1;
        String mccName = "N/A";
        int mnc = -1;
        String mncName = telephonyManager.getNetworkOperatorName();
        if (networkOperator != null && networkOperator.length() > 3) {
            // mobile country code
            mccNo = new Integer(networkOperator.substring(0, 3));
            mccName = telephonyManager.getNetworkCountryIso().toUpperCase();
            // mobile network code
            mnc = new Integer(networkOperator.substring(3));
        }

        // cell type (EDGE, UMTS, LTE, ...
        int cellTypeNo = telephonyManager.getNetworkType();
        String cellTypeName = CellInfoUtils.getCellTypeName(cellTypeNo);

        TextView mainCellText = (TextView) activity.findViewById(R.id.mainCellIdText);

        String mainCellInfo = "Cell ID: " + (cid == -1 ? "N/A" : cid) + "\n" +
                "Cell Type: " + (cellTypeNo == -1 ? "N/A" : cellTypeName + " (" + cellTypeNo + ")") + "\n" +
                "Mobile Country Code: " + (mccNo == -1 ? "N/A" : mccName + " (" + mccNo + ")") + "\n" +
                "Mobile Network Operator: " + (mnc == -1 ? "N/A" : mncName + " (" + mnc + ")") + "\n" +
                "Location Area Code: " + (lac == -1 ? "N/A" : lac) + "\n" +
                "Primary Scrambling Code: " + (psc == -1 ? "N/A" : psc);


        mainCellText.setText(mainCellInfo);

        // refresh signal strength text view
        TextView signalText = (TextView) activity.findViewById(R.id.mainCellSignalStrenghtText);
        signalText.setText(signalStrengthASU + " ASU\n" +
                signalStrengthDBM + " dBm");

        ImageView image = (ImageView) activity.findViewById(R.id.mainCellSignalStrengthImage);

        switch (signalLevel) {
            // no signal
            case -1:
                image.setImageDrawable(activity.getResources().getDrawable(R.mipmap.ic_signal_cellular_off_black_24dp));
                break;
            // poor quality
            case 0:
                image.setImageDrawable(activity.getResources().getDrawable(R.mipmap.ic_signal_cellular_0_bar_black_24dp));
                break;
            case 1:
                image.setImageDrawable(activity.getResources().getDrawable(R.mipmap.ic_signal_cellular_1_bar_black_24dp));
                break;
            case 2:
                image.setImageDrawable(activity.getResources().getDrawable(R.mipmap.ic_signal_cellular_2_bar_black_24dp));
                break;
            case 3:
                image.setImageDrawable(activity.getResources().getDrawable(R.mipmap.ic_signal_cellular_3_bar_black_24dp));
                break;
            // excellent quality
            case 4:
                image.setImageDrawable(activity.getResources().getDrawable(R.mipmap.ic_signal_cellular_4_bar_black_24dp));
                break;
        }
    }

}