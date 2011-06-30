package net.jepstone.NDEFWriteMiFareTag;

import java.nio.charset.Charset;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class NDEFWriteMiFareTagActivity extends Activity {
	
	private Tag mTag; // Represents a tag that we've discovered
    private NfcAdapter mAdapter; // The phone's NFC adapter
    private String[][] mTechLists; // Which RFID tag types to look for
    
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all MIME based dispatches
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            filter.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Failed to add data type for */*", e);
        }
        mFilters = new IntentFilter[] {
                filter,
        };
        
        // Setup a tech list for all MiFare Classic tags
        mTechLists = new String[][] { new String[] { MifareClassic.class.getName() } };

        final Button writeButton = (Button) findViewById(R.id.buttonWrite);
        writeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {    
            	writeTag("Testing 1 2 3");
            }
        });


    }
    
    private void writeTag(String msg) {
    	if (mTag == null) {
    		alertMsg("Null tag received");
    	} else {
    		
    		Ndef ndef = Ndef.get(mTag); // Create an empty NDEF message for this tag type
    	
    		try {
    			ndef.connect(); // Enable IO operation on this tag.

    			// Create a new NDEF message containing this string.
    	    	NdefMessage message = new NdefMessage(
    	    			new NdefRecord[] { newTextRecord(msg, Locale.ENGLISH, true)});

    	    	// Try to write the message.
    			ndef.writeNdefMessage(message);
    			alertMsg("Wrote the text to the tag.");
    			
    			ndef.close();
    			
    		} catch (Exception e) {
    			alertMsg("Failed writing to the tag.");
    		}
    	}
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(mAdapter != null) {
        	mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
        }
    }

    /* Called when a tag is found */
    @Override
    public void onNewIntent(Intent intent) {
        mTag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mAdapter != null) {
        	mAdapter.disableForegroundDispatch(this);
        }
    }
    
   
    /*
     * Taken from newTextRecord() in
     *  http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/nfc/ForegroundNdefPush.html
     *      
     */
    private static NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
    	
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length]; 
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }


    // Display a dialog
    private void alertMsg(String msg) {

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(msg)
    		.setCancelable(false)
    		.setPositiveButton("OK", null);
    	AlertDialog alert = builder.create();
    	alert.show();
    }

}