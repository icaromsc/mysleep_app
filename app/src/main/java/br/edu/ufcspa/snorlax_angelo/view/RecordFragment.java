package br.edu.ufcspa.snorlax_angelo.view;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import br.edu.ufcspa.snorlax_angelo.AppLog;
import br.edu.ufcspa.snorlax_angelo.InfoActivity;
import br.edu.ufcspa.snorlax_angelo.Utilities;
import br.edu.ufcspa.snorlax_angelo.database.DataBaseAdapter;
import br.edu.ufcspa.snorlax_angelo.managers.SharedPreferenceManager;
import br.edu.ufcspa.snorlax_angelo.model.RecordedFiles;
import br.edu.ufcspa.snorlax_angelo.model.Recording;
import ufcspa.edu.br.snorlax_angelo.R;




public class RecordFragment extends Fragment {


    private long record_size = 60000*10; //10 minute
    private long MIN_AVAILABLE_SPACE = 100;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "Snore_angELO";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_RECORDING_TIME = 48;
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    String fileToprocess = "";
    String logApp="app";
    private static final int RESULT_INSTRUCTIONS = 42;
    private int tempNumber=0;
    private int idRecording = 0;
    private int codUser = 0;


    private Chronometer cronometro;
    private Button btn_gravacao;
    private TextView txt_status;
    private AlertDialog alerta;
    private AlertDialog.Builder builder;
    private RelativeLayout recording_message;
    View myView;



    // TODO: Rename parameter arguments, choose names that match
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        myView= inflater.inflate(R.layout.fragment_record, container, false);
        return myView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.title_fragment_record);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
        recording_message = (RelativeLayout) myView.findViewById(R.id.record_message_layout);
        cronometro = (Chronometer) getView().findViewById(R.id.cronometro);


        btn_gravacao = ((Button) getView().findViewById(R.id.btn_gravacao));
        btn_gravacao.setOnClickListener(btnClick);

        txt_status = ((TextView)getView().findViewById(R.id.txt_status));

        builder = new AlertDialog.Builder(getView().getContext());
        builder.setTitle("MySleep");
        builder.setMessage("Recording finished!");
        builder.setCancelable(false);
        builder.setIcon(R.drawable.ic_recording_night);
        //define um botão como positivo
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });//:)
        alerta = builder.create();



    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    private View.OnClickListener btnClick = new View.OnClickListener() {
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {

               if(!isRecording) {
                   if(!isOnline()){
                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                           Toast.makeText(getContext(),"Please, conect your smartphone to Wi-Fi",Toast.LENGTH_LONG).show();
                   }
                   else if(Utilities.getAvailableSpaceInMB() < MIN_AVAILABLE_SPACE){
                       Toast.makeText(getContext(),"Insufficient storage space. It is necessary at least 100MB",Toast.LENGTH_LONG).show();
                   }
                   else {
                       if (SharedPreferenceManager.getSharedInstance().seeInstructions())
                           mountRecording();
                       else {
                           Intent intent = new Intent(getContext(), InfoActivity.class);
                           startActivityForResult(intent, RESULT_INSTRUCTIONS);
                           //mountRecording();
                       }
                   }
               }
               else{
                   dismountRecording();
               }


        }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("result","on Activity resylt record fragment");
        Log.d("result","request code:" +requestCode);
        Log.d("result","result code:" +resultCode);
        if (resultCode==RESULT_INSTRUCTIONS)
            mountRecording();
    }


    /**
     * configura view para inicialização da  gravação
     */
    private void mountRecording(){
        if(getActivity()!=null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
        AppLog.logString("Start Recording");
        recording_message.setVisibility(View.VISIBLE);
        startRecording();
        cronometro.setBase(SystemClock.elapsedRealtime());
        cronometro.start();
        btn_gravacao.setText(getString(R.string.btn_stop));
        txt_status.setText(getString(R.string.recording));
        Toast.makeText(getActivity().getApplicationContext(),"Good night!",Toast.LENGTH_SHORT).show();
    }

    /**
     * configura view para finalização da gravação
     */
    private void dismountRecording(){
        AppLog.logString("Stop Recording");
        recording_message.setVisibility(View.INVISIBLE);
        stopRecording();
        cronometro.stop();
        alerta.show();
        cronometro.setBase(SystemClock.elapsedRealtime());
        btn_gravacao.setText(getString(R.string.btn_start));
        txt_status.setText(getString(R.string.start_capture));
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }


    /**
     * verifica conectividade com intenet
     * @return
     */
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    /**
     *
     * @return nome do arquivo de gravação
     */
    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        String filename=createFilename();
        File tempFile = new File(filepath,filename+".raw");

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + filename+".raw");
    }


    /**
     * configura nome do arquivo de gravação
     * @return
     */
    private String createFilename(){
        tempNumber++;
        /*Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
*/      String strDate = codUser+"_"+idRecording+"_"+tempNumber;
        return strDate;
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onStart() {
        super.onStart();
    }


    /**
     * inicializa thread de gravação
     */
    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                getAudioData();
            }
        },"AudioRecorderActivity Thread");

        recordingThread.start();

    }


    /**
     * Thread de gravação do audio
     */
    private void getAudioData(){
        fileToprocess = "";
        idRecording = saveRecordingOnDatabase();
        DataBaseAdapter data = DataBaseAdapter.getInstance(myView.getContext());
        codUser=data.getUserId();
        while(isRecording) {
            fileToprocess = writeAudioDataToFile(record_size);
            Log.d(logApp,"salvou arquivo, salvando agora no banco");
            saveTempRecordingFile(fileToprocess,idRecording,tempNumber);

            if (tempNumber >= MAX_RECORDING_TIME){
                Log.d(logApp,"ultrapassou max recording...");
                isRecording=false;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppLog.logString("Stop Recording");
                        Log.d(logApp,"stopping recording...");
                        recording_message.setVisibility(View.INVISIBLE);
                        stopRecording();
                        cronometro.stop();

                        alerta.show();

                        cronometro.setBase(SystemClock.elapsedRealtime());

                        btn_gravacao.setText(getString(R.string.btn_start));
                        txt_status.setText(getString(R.string.start_capture));
                    }
                });
                break;
            }
        }
    }


    /**
     * grava trecho de audio
     * @param record_size tempo de gravação
     * @return nome do arquivo gerado
     */
    private String writeAudioDataToFile(long record_size){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;
        Long minuteIni= null;
        Long minuteAtu = null;
        boolean limiteTime = true;
        Calendar c = Calendar.getInstance();
        Date dateStart;
        Date dateAtual;

        try {
            Log.d(logApp,"entrou no try");
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            Log.d(logApp,"entrou no catch");
            e.printStackTrace();
        }

        int read = 0;

        Log.d(logApp,"*** INICIANDO GRAVAÇÃO");
        if(null != os){
            dateStart = c.getTime();
            Log.d(logApp,"data start:"+dateStart.getTime());
            while(isRecording && limiteTime){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){

                    try {
                        c = Calendar.getInstance();
                        dateAtual=c.getTime();
                        if ((dateAtual.getTime() - dateStart.getTime()) > record_size){
                            Log.d(logApp,"entrou no no if para gravar em partes");
                            limiteTime = false;
                        }
                        else{
                            os.write(data);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Log.d(logApp,"os empty");
        }
        return filename;
    }

    /**
     * configura view para finalizar gravação
     */
    private void stopRecording(){
        if(null != recorder){
            isRecording = false;
            recorder.stop();
            recorder.release();
            // update on SQLITE recording status and
            updateRecording(idRecording,getActualDate());
            recorder = null;
            recordingThread = null;
            listRecordings();

        }else{
            Log.d("cycle", "not recording...");
        }

    }

    /**
     * salva arquivo de gravação no banco de dados
     * @param filename
     * @param idRecording
     * @param sequence
     */
    private void saveTempRecordingFile(String filename,int idRecording,Integer sequence){
        Log.d("database","salvando temp recording file[ filename: "+filename+" idRec:"+idRecording+" sequence:"+ sequence +" ]");
        RecordedFiles rec = new RecordedFiles(idRecording,sequence,filename,null);
        DataBaseAdapter data = DataBaseAdapter.getInstance(getActivity());
        data.insertRecordedFile(rec);
    }

    /**
     *
     * @return data formatada
     */
    private String getActualDate(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        String strDate = sdf.format(c.getTime());
        return strDate;

    }

    /**
     * atualiza gravação no banco de dados
     * @param idRecording
     * @param date
     */
    private void updateRecording(int idRecording,String date){
        DataBaseAdapter data = DataBaseAdapter.getInstance(getActivity());
        Recording recording = new Recording(idRecording,null,date,null);
        if(data.updateFinalizeRecording(recording))
            Log.d("database","updated recording[ id: "+idRecording+" stopDate:"+date+" ]");
        else
            Log.e("database","error updating recording");
    }

    /**
     * salva gravação no banco de dados
     * @return
     */
    private int saveRecordingOnDatabase(){
        DataBaseAdapter data = DataBaseAdapter.getInstance(getActivity());
        return data.insertRecording(new Recording(0,getActualDate(),null,null));
    }


    /**
     * lista gravações e arquivos de gravações no logcat
     */
    private void listRecordings() {
        DataBaseAdapter data = DataBaseAdapter.getInstance(getActivity());
        ArrayList<RecordedFiles> recordedFilesArrayList = (ArrayList<RecordedFiles>) data.getRecordedFiles();
        ArrayList<Recording> recordingArrayList = (ArrayList<Recording>) data.getRecordings();

        if (recordedFilesArrayList.size() > 0) {
            for (RecordedFiles f : recordedFilesArrayList) {
                Log.d("database", "recorded file: " + f.toString());
            }
        }
        if (recordingArrayList.size()>0){
            for(Recording r : recordingArrayList){
                Log.d("database", "recording: " + r.toString());
            }
        }
    }


    @Override
    public void onDestroy() {
        stopRecording();
        Log.d("cycle", "app closed, saving recording...");
        super.onDestroy();
    }




}
