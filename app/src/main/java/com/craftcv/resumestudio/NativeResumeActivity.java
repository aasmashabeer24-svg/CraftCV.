package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Offline-first Android resume studio. This screen does not load AppDeploy.
 * Records and templates live in app-private storage; no login or tracking.
 */
public final class NativeResumeActivity extends Activity {
    private static final String PREFS = "craftcv_native_resumes_v1";
    private static final String KEY_ITEMS = "resumes";
    private static final String KEY_CURRENT = "current";
    private static final int CREATE_PDF = 411;
    private static final int CREATE_DOCX = 412;
    private static final int CHOOSE_PHOTO = 413;
    private byte[] pendingDocx;
    private byte[] pendingPdf;
    private Uri lastSavedPdf;
    private final String[] fields = {"Full name", "Professional title", "Email", "Phone", "Location",
            "Professional summary", "Work experience", "Education", "Skills", "Projects"};
    private final String[] keys = {"name","role","email","phone","location","summary","experience","education","skills","projects"};
    private final EditText[] inputs = new EditText[keys.length];
    private final int[] colors = {0xFF194C5D,0xFF3C584D,0xFF774960,0xFF303D48,0xFF274054,0xFF354458,0xFF417C69,0xFFB05241,0xFF3E5068,0xFF317071,0xFF906D59,0xFF252525};
    private final String[] themes = {"Editorial","Modern","Creative","Minimal","Executive","Classic","Fresh","Bold","Academic","Compact","Elegant","Monochrome"};
    private int style = 0;
    private String currentId = "";
    private String photoPath = "";
    // Structured records are mirrored to the legacy text fields for existing exports.
    private JSONArray educationEntries = new JSONArray();
    private JSONArray skillGroups = new JSONArray();
    private ImageView photoPreview;
    private TextView photoStatus;
    private LinearLayout content;
    private TextView preview;
    private TextView status;
    private boolean loading = false;
    private int activeStep=0;
    private final LinearLayout[] stepPanels=new LinearLayout[4];
    private ScrollView formScroll;
    private LinearLayout progressTrack;
    private TextView progressCaption;
    private TextView selectedDesign;
    private Button backStepButton;
    private Button continueStepButton;
    private final android.os.Handler saveHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable savePending=()->{if(!loading)save();};

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17,44,60));
        getWindow().setNavigationBarColor(Color.rgb(17,44,60));
        buildUI();
        JSONObject initial = null;
        try {
            JSONArray array = readItems();
            String wanted = preferences().getString(KEY_CURRENT,"");
            for(int i=0;i<array.length();i++) {
                JSONObject item=array.optJSONObject(i);
                if(item!=null && wanted.equals(item.optString("id"))) { initial=item; break; }
            }
            if(initial==null && array.length()>0) initial=array.optJSONObject(0);
        } catch(Exception ignored) {}
        if(getIntent().getBooleanExtra("start_new",false)){
            createNew(false);
        }else if(initial!=null) load(initial);
        else createNew(false);
        showStep(0);
    }

    private SharedPreferences preferences() { return getSharedPreferences(PREFS,MODE_PRIVATE); }
    private JSONArray readItems() {
        try {return new JSONArray(preferences().getString(KEY_ITEMS,"[]"));}
        catch(Exception ignored) {return new JSONArray();}
    }
    private int dp(int n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size,int color) {
        TextView view=new TextView(this);
        view.setText(value);view.setTextSize(size);view.setTextColor(color);
        return view;
    }
    private Button action(String name, Runnable command) {
        Button b=new Button(this);
        b.setText(name);b.setAllCaps(false);b.setTextSize(12);
        b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(31,83,99)));
        b.setOnClickListener(v->command.run());
        return b;
    }
    private android.graphics.drawable.GradientDrawable background(int color,int radius,int stroke){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);
        return d;
    }
    private TextView heading(String value,int size,int color,boolean bold){
        TextView t=label(value,size,color);
        if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        return t;
    }
    private Button toolbarButton(String value,Runnable task,boolean filled){
        Button b=new Button(this);
        b.setText(value);b.setAllCaps(false);b.setTextSize(12);
        b.setTextColor(filled?Color.WHITE:0xFF26505A);
        b.setBackgroundTintList(null);
        b.setBackground(background(filled?0xFF246D65:0xFFEAF3F2,11,0));
        b.setOnClickListener(v->task.run());return b;
    }
    private void openDesignPage(){
        saveHandler.removeCallbacks(savePending);
        save();
        Intent intent=new Intent(this,ResumeTemplatesActivity.class);
        intent.putExtra("return_to_editor",true);
        startActivity(intent);
    }
    private void openEducationSkills(){
        saveHandler.removeCallbacks(savePending);
        save();
        Intent intent=new Intent(this,EducationSkillsActivity.class);
        intent.putExtra("resume_id",currentId);
        startActivity(intent);
    }
    private void addSectionHeading(LinearLayout panel,String title,String description){
        TextView h=heading(title,24,0xFF193D4B,true);
        panel.addView(h);
        TextView sub=heading(description,13,0xFF7A9098,false);
        sub.setLineSpacing(dp(2),1.0f);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(8);p.bottomMargin=dp(15);
        panel.addView(sub,p);
    }
    private void addField(LinearLayout panel,int index){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(13),dp(14),dp(11));
        card.setBackground(background(Color.WHITE,14,0xFFE1EAED));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
        cp.bottomMargin=dp(12);panel.addView(card,cp);
        TextView label=heading(fields[index],12,0xFF325766,true);
        label.setPadding(dp(2),0,0,dp(6));card.addView(label);
        EditText input=new EditText(this);
        input.setTextSize(14);
        input.setTextColor(0xFF203F4B);
        input.setHintTextColor(0xFFA3B2B6);
        input.setPadding(dp(12),dp(9),dp(12),dp(9));
        input.setBackground(background(0xFFF7FAFB,9,0xFFE6ECEE));
        boolean multiline=index>=5;
        input.setSingleLine(!multiline);
        if(multiline){
            input.setMinLines(index==5?4:3);
            input.setGravity(Gravity.TOP);
            input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        if(index==2)input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if(index==3)input.setInputType(InputType.TYPE_CLASS_PHONE);
        String[] hints={
            "Your full name","e.g. Software Developer","you@example.com","+92 3XX XXXXXXX","City, country",
            "A few lines about your actual experience and goals",
            "Job title, organization, dates and achievements",
            "Qualification, institute and year","Separate your skills with commas or new lines",
            "Project name and the work you completed"
        };
        input.setHint(hints[index]);
        card.addView(input,new LinearLayout.LayoutParams(-1,-2));
        inputs[index]=input;
        input.addTextChangedListener(new android.text.TextWatcher(){
            @Override public void beforeTextChanged(CharSequence value,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence value,int start,int before,int count){}
            @Override public void afterTextChanged(android.text.Editable editable){
                if(loading)return;
                saveHandler.removeCallbacks(savePending);
                saveHandler.postDelayed(savePending,900);
            }
        });
    }

    private void addPhotoChooser(LinearLayout panel){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(14),dp(14),dp(14));
        card.setBackground(background(Color.WHITE,14,0xFFE1EAED));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(13);
        panel.addView(card,p);
        card.addView(heading("Profile photo (optional)",15,0xFF325766,true));
        photoStatus=heading("Add a photo for a photo-style CV; ATS resumes can remain photo-free.",12,0xFF78909B,false);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
        sp.topMargin=dp(6);
        card.addView(photoStatus,sp);
        photoPreview=new ImageView(this);
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoPreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(84),dp(84));
        pp.topMargin=dp(10);
        card.addView(photoPreview,pp);
        LinearLayout actions=new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(47));
        ap.topMargin=dp(10);
        card.addView(actions,ap);
        actions.addView(toolbarButton("Choose photo",this::choosePhoto,false),new LinearLayout.LayoutParams(0,dp(47),1));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(47),1);
        rp.leftMargin=dp(8);
        actions.addView(toolbarButton("Remove",()->{
            photoPath="";
            refreshPhoto();
            save();
        },false),rp);
    }
    private void choosePhoto(){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try{
            startActivityForResult(Intent.createChooser(intent,"Choose resume photo"),CHOOSE_PHOTO);
        }catch(Exception error){
            Toast.makeText(this,"No photo picker is installed",Toast.LENGTH_LONG).show();
        }
    }
    private void refreshPhoto(){
        if(photoStatus==null || photoPreview==null)return;
        File file=photoPath.isEmpty()?null:new File(photoPath);
        if(file==null || !file.isFile()){
            photoStatus.setText("No photo selected. Your CV works without one.");
            photoPreview.setImageDrawable(null);
            photoPreview.setVisibility(View.GONE);
            return;
        }
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize=2;
        Bitmap bitmap=BitmapFactory.decodeFile(photoPath,options);
        if(bitmap==null){
            photoStatus.setText("Photo unavailable. Choose another.");
            photoPreview.setVisibility(View.GONE);
            return;
        }
        photoPreview.setImageBitmap(bitmap);
        photoPreview.setVisibility(View.VISIBLE);
        photoStatus.setText("Photo is saved privately on this phone.");
    }
    private void importPhoto(Uri imageUri){
        try{
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            try(InputStream input=getContentResolver().openInputStream(imageUri)){
                if(input==null)throw new IllegalStateException("Image cannot be opened");
                BitmapFactory.decodeStream(input,null,bounds);
            }
            if(bounds.outWidth<1 || bounds.outHeight<1)throw new IllegalStateException("Invalid image");
            BitmapFactory.Options opts=new BitmapFactory.Options();
            opts.inSampleSize=1;
            int largest=Math.max(bounds.outWidth,bounds.outHeight);
            while(largest/opts.inSampleSize>1150)opts.inSampleSize*=2;
            Bitmap bitmap;
            try(InputStream input=getContentResolver().openInputStream(imageUri)){
                bitmap=BitmapFactory.decodeStream(input,null,opts);
            }
            if(bitmap==null)throw new IllegalStateException("Image could not be decoded");
            int orientation=1;
            try(InputStream input=getContentResolver().openInputStream(imageUri)){
                if(input!=null){
                    android.media.ExifInterface exif=new android.media.ExifInterface(input);
                    orientation=exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);
                }
            }catch(Exception ignored){}
            int degrees=orientation==6?90:orientation==3?180:orientation==8?270:0;
            if(degrees!=0){
                Matrix matrix=new Matrix();
                matrix.postRotate(degrees);
                Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                if(rotated!=bitmap)bitmap.recycle();
                bitmap=rotated;
            }
            int width=bitmap.getWidth(),height=bitmap.getHeight();
            float ratio=Math.min(1f,720f/Math.max(width,height));
            if(ratio<1f){
                Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.round(width*ratio),Math.round(height*ratio),true);
                if(scaled!=bitmap)bitmap.recycle();
                bitmap=scaled;
            }
            File directory=new File(getFilesDir(),"resume-photos");
            if(!directory.exists() && !directory.mkdirs())throw new IllegalStateException("Photo storage unavailable");
            File file=new File(directory,"photo-"+System.currentTimeMillis()+".jpg");
            try(FileOutputStream output=new FileOutputStream(file)){
                if(!bitmap.compress(Bitmap.CompressFormat.JPEG,86,output))throw new IllegalStateException("Photo conversion failed");
            }
            bitmap.recycle();
            photoPath=file.getAbsolutePath();
            refreshPhoto();
            save();
            Toast.makeText(this,"Photo added to your CV",Toast.LENGTH_SHORT).show();
        }catch(Exception error){
            Toast.makeText(this,"Photo import failed: "+error.getMessage(),Toast.LENGTH_LONG).show();
        }catch(OutOfMemoryError memory){
            Toast.makeText(this,"Image too large. Choose a smaller photo.",Toast.LENGTH_LONG).show();
        }
    }
    private void showStep(int next){
        activeStep=Math.max(0,Math.min(3,next));
        for(int i=0;i<stepPanels.length;i++){
            if(stepPanels[i]!=null)stepPanels[i].setVisibility(i==activeStep?View.VISIBLE:View.GONE);
        }
        if(progressCaption!=null)progressCaption.setText("STEP "+(activeStep+1)+" OF 4");
        if(progressTrack!=null){
            progressTrack.removeAllViews();
            for(int i=0;i<4;i++){
                View piece=new View(this);
                piece.setBackground(background(i<=activeStep?0xFF2A897C:0xFFE3EAEC,8,0));
                LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(6),1);
                if(i!=0)pp.leftMargin=dp(6);
                progressTrack.addView(piece,pp);
            }
        }
        if(backStepButton!=null){
            backStepButton.setText(activeStep==0?"‹ Home":"‹ Previous");
        }
        if(continueStepButton!=null){
            continueStepButton.setText(activeStep==3?"Finish & View CV  →":"Continue  →");
        }
        if(selectedDesign!=null)selectedDesign.setText("Current design: "+themes[style]);
        if(formScroll!=null)formScroll.post(()->formScroll.scrollTo(0,0));
    }
    private void forward(){
        saveHandler.removeCallbacks(savePending);
        save();
        if(activeStep<3){showStep(activeStep+1);return;}
        if(inputs[0].getText().toString().trim().isEmpty()){
            showStep(0);
            inputs[0].setError("Enter your name before finishing");
            inputs[0].requestFocus();
            Toast.makeText(this,"Add your full name to finish your CV",Toast.LENGTH_LONG).show();
            return;
        }
        openFinishedCV();
    }
    private void backward(){
        saveHandler.removeCallbacks(savePending);
        save();
        if(activeStep==0)finish();
        else showStep(activeStep-1);
    }
    private void buildUI() {
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F8F9);
        setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18),dp(13),dp(18),dp(15));
        header.setBackground(background(Color.WHITE,0,0));
        root.addView(header);

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);header.addView(top);
        Button home=toolbarButton("‹  Home",this::backward,false);
        top.addView(home,new LinearLayout.LayoutParams(dp(99),dp(42)));
        TextView title=heading("Build My CV",21,0xFF193D4B,true);
        title.setGravity(Gravity.CENTER);
        top.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));
        Button design=toolbarButton("Design",this::openDesignPage,false);
        top.addView(design,new LinearLayout.LayoutParams(dp(87),dp(42)));

        progressCaption=heading("STEP 1 OF 4",10,0xFF398579,true);
        progressCaption.setLetterSpacing(.13f);
        LinearLayout.LayoutParams pc=new LinearLayout.LayoutParams(-1,-2);
        pc.topMargin=dp(18);pc.bottomMargin=dp(7);
        header.addView(progressCaption,pc);
        progressTrack=new LinearLayout(this);progressTrack.setOrientation(LinearLayout.HORIZONTAL);
        header.addView(progressTrack,new LinearLayout.LayoutParams(-1,dp(6)));
        selectedDesign=heading("Current design: "+themes[style],11,0xFF668A8B,false);
        selectedDesign.setPadding(0,dp(9),0,0);
        header.addView(selectedDesign);

        formScroll=new ScrollView(this);formScroll.setFillViewport(true);
        root.addView(formScroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout allSteps=new LinearLayout(this);
        allSteps.setOrientation(LinearLayout.VERTICAL);
        allSteps.setPadding(dp(16),dp(24),dp(16),dp(25));
        formScroll.addView(allSteps);
        for(int i=0;i<stepPanels.length;i++){
            stepPanels[i]=new LinearLayout(this);
            stepPanels[i].setOrientation(LinearLayout.VERTICAL);
            allSteps.addView(stepPanels[i]);
        }

        addSectionHeading(stepPanels[0],"Personal details","First, tell us how employers can contact you.");
        for(int i=0;i<=4;i++)addField(stepPanels[0],i);
        addPhotoChooser(stepPanels[0]);

        TextView helper=heading("Applying for your first job? Start as a fresher — you do not need to invent work experience.",12,0xFF587A83,false);
        helper.setPadding(dp(6),dp(6),dp(6),dp(12));
        stepPanels[0].addView(helper);
        Button starter=toolbarButton("Start as student / fresher",this::studentStarter,false);
        stepPanels[0].addView(starter,new LinearLayout.LayoutParams(-1,dp(48)));

        addSectionHeading(stepPanels[1],"Profile & experience","Summarize your strengths and add only genuine work, freelance or internship experience.");
        addField(stepPanels[1],5);
        addField(stepPanels[1],6);

        addSectionHeading(stepPanels[2],"Education & skills","Add your qualifications and the skills relevant to your target job.");
        addField(stepPanels[2],7);
        addField(stepPanels[2],8);
        // Keep legacy text values available to PDF/DOCX/AI, but edit them in
        // the dedicated repeatable Education & Skills screen.
        ((View)inputs[7].getParent()).setVisibility(View.GONE);
        ((View)inputs[8].getParent()).setVisibility(View.GONE);
        TextView structuredNote=heading("Add multiple qualifications and organize skills into separate categories. Your existing CV details remain compatible.",12,0xFF66818A,false);
        structuredNote.setPadding(dp(5),dp(5),dp(5),dp(12));
        stepPanels[2].addView(structuredNote);
        Button manageEducationSkills=toolbarButton("Manage Education & Skill Categories",this::openEducationSkills,true);
        stepPanels[2].addView(manageEducationSkills,new LinearLayout.LayoutParams(-1,dp(51)));

        addSectionHeading(stepPanels[3],"Projects & finishing","Add your best projects, check the CV, then see the finished document.");
        addField(stepPanels[3],9);
        Button check=toolbarButton("Check my resume",this::resumeCheck,false);
        LinearLayout.LayoutParams chk=new LinearLayout.LayoutParams(-1,dp(52));
        chk.topMargin=dp(12);stepPanels[3].addView(check,chk);
        TextView finalNote=heading("Your resume is saved automatically. The next page will show the actual A4 CV, with Save PDF, Word and Share options.",12,0xFF6B858F,false);
        finalNote.setPadding(dp(8),dp(18),dp(8),0);
        stepPanels[3].addView(finalNote);

        LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(16),dp(8),dp(16),dp(13));
        footer.setBackground(background(Color.WHITE,0,0));
        root.addView(footer);
        status=heading("Your progress saves on this phone.",11,0xFF478476,false);
        status.setGravity(Gravity.CENTER);
        footer.addView(status,new LinearLayout.LayoutParams(-1,dp(23)));
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER_VERTICAL);
        footer.addView(nav,new LinearLayout.LayoutParams(-1,dp(52)));
        backStepButton=toolbarButton("‹ Home",this::backward,false);
        LinearLayout.LayoutParams prev=new LinearLayout.LayoutParams(0,dp(50),1);
        prev.rightMargin=dp(8);nav.addView(backStepButton,prev);
        continueStepButton=toolbarButton("Continue  →",this::forward,true);
        nav.addView(continueStepButton,new LinearLayout.LayoutParams(0,dp(50),2));
        showStep(0);
    }
    private void addCommand(LinearLayout row,String title,Runnable callback) {
        Button b=action(title,callback);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(49),1);
        params.setMargins(0,0,dp(5),0);
        row.addView(b,params);
    }
    private JSONObject snapshot() {
        JSONObject item=new JSONObject();
        try {
            item.put("id",currentId);item.put("style",style);
            item.put("photoPath",photoPath);
            item.put("educationEntries",educationEntries);
            item.put("skillGroups",skillGroups);
            for(int i=0;i<keys.length;i++)item.put(keys[i],inputs[i].getText().toString());
        } catch(Exception ignored){}
        return item;
    }
    private void load(JSONObject item) {
        loading=true;
        currentId=item.optString("id");
        photoPath=item.optString("photoPath","");
        refreshPhoto();
        style=Math.max(0,Math.min(themes.length-1,item.optInt("style",0)));
        educationEntries=item.optJSONArray("educationEntries");
        if(educationEntries==null)educationEntries=new JSONArray();
        skillGroups=item.optJSONArray("skillGroups");
        if(skillGroups==null)skillGroups=new JSONArray();
        for(int i=0;i<keys.length;i++){
            String loaded=item.optString(keys[i],"");
            // Repair records created by the old duplicate bug, which wrote "(Copy)" as the person's name.
            if(i==0 && "(Copy)".equals(loaded.trim()) && item.optString("role","").trim().isEmpty()){
                loaded="";
            }
            inputs[i].setText(loaded);
        }
        loading=false;updatePreview();
        if(selectedDesign!=null)selectedDesign.setText("Current design: "+themes[style]);
        preferences().edit().putString(KEY_CURRENT,currentId).apply();
    }
    private void createNew(boolean confirm) {
        saveHandler.removeCallbacks(savePending);
        if(confirm){new AlertDialog.Builder(this).setMessage("Start a new resume? Unsaved changes will be lost.").setPositiveButton("Create",(d,w)->createNew(false)).setNegativeButton("Cancel",null).show();return;}
        currentId="native-"+System.currentTimeMillis();
        photoPath="";
        educationEntries=new JSONArray();
        skillGroups=new JSONArray();
        refreshPhoto();
        loading=true;
        style=preferences().getInt("default_style",0);
        for(EditText input:inputs)input.setText("");
        loading=false;
        showStep(0);
        updatePreview();status.setText("New resume created. Tap Save when ready.");
    }
    private void save() {
        if(loading)return;
        try {
            JSONArray old=readItems(),next=new JSONArray();
            for(int i=0;i<old.length();i++){
                JSONObject item=old.optJSONObject(i);
                if(item!=null && !currentId.equals(item.optString("id")))next.put(item);
            }
            next.put(snapshot());
            preferences().edit().putString(KEY_ITEMS,next.toString()).putString(KEY_CURRENT,currentId).apply();
            status.setText("Saved privately on this device.");
            updatePreview();
        }catch(Exception e){status.setText("Could not save resume.");}
    }
    private void studentStarter(){
        new AlertDialog.Builder(this).setTitle("New student / fresher CV?")
            .setMessage("Create a blank first-job CV without fake work history. Unsaved changes in the current CV will be replaced.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Create",(dialog,which)->{
                saveHandler.removeCallbacks(savePending);
                save(); // Preserve the previous resume before switching to a fresh starter.
                createNew(false);
                inputs[1].setText("Student / Fresh Graduate");
                inputs[6].setText("");
                inputs[7].setText("");
                inputs[8].setText("");
                status.setText("Add education, projects and genuine skills. You do not need job experience.");
                save();
            }).show();
    }
    private void resumeCheck(){
        java.util.ArrayList<String> suggestions=new java.util.ArrayList<>();
        if(value(0,"").isEmpty())suggestions.add("Add your full name.");
        if(value(1,"").isEmpty())suggestions.add("Add a professional title or target role.");
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(value(2,"")).matches())suggestions.add("Add a valid email.");
        if(value(5,"").length()<45)suggestions.add("Write a short, factual summary (at least 45 characters).");
        if(value(7,"").isEmpty())suggestions.add("Add your education or relevant qualification.");
        if(value(8,"").isEmpty())suggestions.add("List skills relevant to the job.");
        if(value(6,"").isEmpty() && value(9,"").isEmpty())suggestions.add("Include an internship, project or real experience if available.");
        String result=suggestions.isEmpty()?"All basic CV completeness checks passed. Review your claims and PDF layout before applying.":"Suggested improvements:\n\n• "+android.text.TextUtils.join("\n• ",suggestions);
        new AlertDialog.Builder(this).setTitle("Resume completeness (not an ATS guarantee)")
            .setMessage(result).setPositiveButton("OK",null).show();
    }
    private void surpriseStyle(){
        int next=(int)(Math.random()*themes.length);
        if(next==style)next=(next+1)%themes.length;
        style=next;updatePreview();save();
        Toast.makeText(this,"New style: "+themes[style],Toast.LENGTH_SHORT).show();
    }
    private void openCoverLetter(){
        Intent intent=new Intent(this,CoverLetterActivity.class);
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_NAME,value(0,""));
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_ROLE,value(1,""));
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_SUMMARY,value(5,""));
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_SKILLS,value(8,""));
        startActivity(intent);
    }
    private void openFinishedCV(){
        saveHandler.removeCallbacks(savePending);
        save();
        startActivity(new Intent(this,ResumePreviewActivity.class));
    }
    private void duplicateResume(){
        save();
        JSONObject copy=snapshot();
        currentId="native-"+System.currentTimeMillis()+"-copy";
        // A duplicate must not alter the applicant's actual name.
        inputs[0].setText(copy.optString("name",""));
        save();
        status.setText("Duplicated. Your original remains saved.");
    }
    private void deleteResume(){
        JSONArray items=readItems();
        if(items.length()<2){Toast.makeText(this,"Keep one CV. Create another before deleting.",Toast.LENGTH_LONG).show();return;}
        new AlertDialog.Builder(this).setTitle("Delete this resume?")
            .setMessage("This removes the selected local resume from this phone. Keep a PDF backup.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Delete",(dialog,which)->{
                try {
                    JSONArray remaining=new JSONArray();
                    for(int i=0;i<items.length();i++){
                        JSONObject item=items.optJSONObject(i);
                        if(item!=null && !currentId.equals(item.optString("id")))remaining.put(item);
                    }
                    preferences().edit().putString(KEY_ITEMS,remaining.toString()).apply();
                    load(remaining.getJSONObject(0));
                    status.setText("Local resume deleted.");
                }catch(Exception e){status.setText("Could not delete resume.");}
            }).show();
    }
    private void sharePdf(){
        if(lastSavedPdf==null){Toast.makeText(this,"Export and save a PDF first, then tap Share PDF.",Toast.LENGTH_LONG).show();return;}
        try{
            Intent share=new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM,lastSavedPdf);
            share.setClipData(android.content.ClipData.newUri(getContentResolver(),"CraftCV PDF",lastSavedPdf));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"Share your CraftCV resume"));
        }catch(Exception e){Toast.makeText(this,"Cannot share this PDF. Try saving a new copy.",Toast.LENGTH_LONG).show();}
    }
    private void chooseResume(){
        JSONArray items=readItems();
        if(items.length()==0){Toast.makeText(this,"Save your first CV to see it here",Toast.LENGTH_SHORT).show();return;}
        String[] names=new String[items.length()];
        for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);
            names[i]=item!=null ? (item.optString("name","").trim().isEmpty()?"Untitled CV":item.optString("name"))+
                (item.optString("id","").endsWith("-copy")?" (copy)":"")+" — "+item.optString("role","") : "Resume";
        }
        new AlertDialog.Builder(this).setTitle("My saved resumes").setItems(names,(dialog,which)->{
            JSONObject selected=items.optJSONObject(which);
            if(selected!=null)load(selected);
        }).setPositiveButton("Done",null).show();
    }
    private void updatePreview(){
        if(preview==null)return;
        StringBuilder b=new StringBuilder();
        b.append(value(0,"Your Name").toUpperCase(Locale.ROOT)).append("\n");
        b.append(value(1,"Your Professional Title")).append("\n");
        b.append(value(2,"")).append("  ").append(value(3,"")).append("\n");
        for(int i=5;i<keys.length;i++){
            String line=value(i,"").trim();
            if(line.isEmpty())continue;
            b.append("\n").append(fields[i].toUpperCase(Locale.ROOT)).append("\n");
            b.append(line).append("\n");
        }
        preview.setText(b.toString());
        preview.setTextColor(colors[style]);
        preview.setTextSize(style==2||style==7?16:style==9?13:14);
        preview.setTypeface(null,style==4||style==5?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
    }
    private String value(int index,String fallback){
        String text=inputs[index].getText().toString().trim();
        return text.isEmpty()?fallback:text;
    }
    private void exportDocx(){
        if(value(0,"").trim().isEmpty()){
            Toast.makeText(this,"Add your name before exporting",Toast.LENGTH_LONG).show();return;
        }
        save();
        try{
            pendingDocx=DocxExport.create(snapshot());
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.putExtra(Intent.EXTRA_TITLE,"CraftCV-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".docx");
            startActivityForResult(intent,CREATE_DOCX);
        }catch(Exception e){pendingDocx=null;status.setText("Word export failed: "+e.getMessage());}
    }
    private java.util.ArrayList<String> wrapPdf(String raw,Paint paint,float width){
        java.util.ArrayList<String> output=new java.util.ArrayList<>();
        for(String paragraph:raw.split("\\n",-1)){
            if(paragraph.trim().isEmpty()){output.add("");continue;}
            StringBuilder line=new StringBuilder();
            for(String word:paragraph.trim().split("\\s+")){
                if(paint.measureText(word)>width){
                    if(line.length()>0){output.add(line.toString());line.setLength(0);}
                    for(int j=0;j<word.length();j++){
                        String next=line.toString()+word.charAt(j);
                        if(paint.measureText(next)>width && line.length()>0){output.add(line.toString());line.setLength(0);}
                        line.append(word.charAt(j));
                    }
                }else{
                    String next=line.length()==0?word:line+" "+word;
                    if(paint.measureText(next)>width && line.length()>0){output.add(line.toString());line.setLength(0);}
                    if(line.length()>0)line.append(' ');
                    line.append(word);
                }
            }
            if(line.length()>0)output.add(line.toString());
        }
        return output;
    }
    private void exportPDF(){
        if(value(0,"").trim().isEmpty()){Toast.makeText(this,"Enter your name before exporting",Toast.LENGTH_LONG).show();return;}
        save();
        PdfDocument pdf=new PdfDocument();
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNo=1;
        PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());
        Canvas canvas=page.getCanvas();
        int y=57;
        int primary=colors[style];
        if(style==2||style==4||style==7){
            paint.setColor(primary);
            canvas.drawRect(0,0,595,27,paint);
        }
        paint.setTypeface(android.graphics.Typeface.create(style==0||style==5||style==10?"serif":"sans-serif",android.graphics.Typeface.BOLD));
        paint.setTextSize(style==7?28:style==9?20:24);paint.setColor(style==11?Color.BLACK:primary);
        for(String line:wrapPdf(value(0,"Your Name"),paint,500)){canvas.drawText(line,45,y,paint);y+=30;}
        paint.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
        paint.setTextSize(13);
        for(String line:wrapPdf(value(1,""),paint,500)){canvas.drawText(line,45,y,paint);y+=18;}
        paint.setTextSize(10);paint.setColor(Color.DKGRAY);
        for(String line:wrapPdf(value(2,"")+"  •  "+value(3,"")+"  •  "+value(4,""),paint,500)){canvas.drawText(line,45,y,paint);y+=15;}
        y+=13;paint.setColor(primary);paint.setStrokeWidth(1.4f);canvas.drawLine(45,y,550,y,paint);y+=28;
        for(int i=5;i<keys.length;i++){
            String contents=value(i,"").trim();if(contents.isEmpty())continue;
            if(y>730){pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=58;}
            paint.setColor(style==11?Color.BLACK:primary);paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(style==9?11:12);
            canvas.drawText(fields[i].toUpperCase(Locale.ROOT),45,y,paint);y+=20;
            paint.setTypeface(android.graphics.Typeface.DEFAULT);paint.setColor(Color.rgb(50,55,60));paint.setTextSize(10);
            for(String line:wrapPdf(contents,paint,500)){
                if(y>780){pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=58;}
                if(!line.isEmpty())canvas.drawText(line,45,y,paint);
                y+=line.isEmpty()?9:(style==9?14:16);
            }
            y+=style==9?8:15;
        }
        pdf.finishPage(page);
        try {
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            pdf.writeTo(buffer);
            pendingPdf=buffer.toByteArray();
            Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            save.addCategory(Intent.CATEGORY_OPENABLE);
            save.setType("application/pdf");
            save.putExtra(Intent.EXTRA_TITLE,"CraftCV-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".pdf");
            startActivityForResult(save,CREATE_PDF);
        }catch(Exception e){pendingPdf=null;status.setText("PDF export failed: "+e.getMessage());}
        finally {pdf.close();}
    }
    @Override protected void onResume(){
        super.onResume();
        if(currentId==null || currentId.isEmpty() || inputs[0]==null)return;
        JSONObject current=null;
        try{
            JSONArray items=readItems();
            for(int i=0;i<items.length();i++){
                JSONObject row=items.optJSONObject(i);
                if(row!=null && currentId.equals(row.optString("id"))){
                    current=row;
                    style=Math.max(0,Math.min(themes.length-1,row.optInt("style",style)));
                    break;
                }
            }
        }catch(Exception ignored){}
        if(current!=null && !loading){
            loading=true;
            educationEntries=current.optJSONArray("educationEntries");
            if(educationEntries==null)educationEntries=new JSONArray();
            skillGroups=current.optJSONArray("skillGroups");
            if(skillGroups==null)skillGroups=new JSONArray();
            if(inputs[7]!=null)inputs[7].setText(current.optString("education",""));
            if(inputs[8]!=null)inputs[8].setText(current.optString("skills",""));
            loading=false;
            updatePreview();
        }
        if(selectedDesign!=null)selectedDesign.setText("Current design: "+themes[style]);
    }
    @Override protected void onPause(){
        saveHandler.removeCallbacks(savePending);
        save();
        super.onPause();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==CHOOSE_PHOTO){
            if(resultCode==RESULT_OK && data!=null && data.getData()!=null)importPhoto(data.getData());
            return;
        }
        if(requestCode==CREATE_DOCX){
            if(resultCode==RESULT_OK && data!=null && data.getData()!=null && pendingDocx!=null){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null)throw new IllegalStateException("Could not open document");
                    out.write(pendingDocx);
                    status.setText("Editable Word DOCX saved to selected location.");
                }catch(Exception e){status.setText("Word save failed.");}
            }
            pendingDocx=null;
            return;
        }
        if(requestCode!=CREATE_PDF)return;
        if(resultCode==RESULT_OK && data!=null && data.getData()!=null && pendingPdf!=null){
            try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){
                if(stream==null)throw new IllegalStateException("No file output stream");
                stream.write(pendingPdf);
                lastSavedPdf=data.getData();
                status.setText("PDF saved. Tap Share PDF to send it to WhatsApp or email.");
                Toast.makeText(this,"Resume PDF saved",Toast.LENGTH_LONG).show();
            }catch(Exception e){status.setText("PDF save failed: "+e.getMessage());}
        }else{status.setText("PDF save canceled.");}
        pendingPdf=null;
    }
    private String fit(String s,int max){return s.length()>max?s.substring(0,max-1)+"…":s;}
}
