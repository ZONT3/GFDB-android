package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import ru.zont.gfdb.core.CraftRecipe;
import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.Parser;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.data.Crafts;

public class CraftActivity
        extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener, NumberPicker.OnValueChangeListener,
                AdapterView.OnItemSelectedListener {
    private static final int[] RARITY_TABLE_COLOR = {-1, -1, R.color.rarity_common, R.color.rarity_rare,
            R.color.rarity_epic, R.color.rarity_legend, R.color.rarity_extra};
    private static final String[] RARITY_TABLE = {"", "", "★★", "★★★", "★★★★", "★★★★★", "EXTRA"};
    private static final int[] CONSTRUCTION_TYPES = {
            R.string.craft_constype_normal, R.string.craft_constype_large1,
            R.string.craft_constype_large2, R.string.craft_constype_large3};
    static final int REQUEST_SETDOLL = 228;
    static final int OPTION_MINIMUM = 0;
    static final int OPTION_RECCOMEND = 1;
    private static final long ANIM_DELAY = 400;

    private ViewGroup content;
    private ViewGroup loading;

    private NumberPicker tries;
    private NumberPicker parts;
    private NumberPicker rat;
    private NumberPicker ammo;
    private NumberPicker mp;
    private SeekBar craftType;
    private TextView craftTypeTW;
    private TextView cost;
    private TextView chances;
    private ProgressBar craftablePb;
    private TextView craftable;
    private Button set;

    private Thread craftableCheckerThread;

    private TDoll dollToParse;
    private ViewGroup dollView;
    private Spinner option;
    private TextView dollName;
    private TextView dollMeta;
    private ImageView dollThumb;

    private int colorNormal;
    private long lastAnim;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_craft);
        Toolbar toolbar = findViewById(R.id.craft_tb);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        assert supportActionBar != null;
        supportActionBar.setDisplayShowHomeEnabled(true);
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        content = findViewById(R.id.craft_content);
        loading = findViewById(R.id.craft_loading);
        mp = findViewById(R.id.craft_mp);
        ammo = findViewById(R.id.craft_ammo);
        rat = findViewById(R.id.craft_rat);
        parts = findViewById(R.id.craft_parts);
        tries = findViewById(R.id.craft_tries);
        craftType = findViewById(R.id.craft_lc);
        craftTypeTW = findViewById(R.id.craft_lc_tw);
        cost = findViewById(R.id.craft_cost);
        chances = findViewById(R.id.craft_chances);
        craftablePb = findViewById(R.id.craft_craftable_pb);
        craftable = findViewById(R.id.craft_craftable);
        dollView = findViewById(R.id.craft_doll);
        option = findViewById(R.id.craft_doll_option);
        dollName = findViewById(R.id.craft_doll_name);
        dollMeta = findViewById(R.id.craft_doll_meta);
        dollThumb = findViewById(R.id.craft_doll_thumb);
        set = findViewById(R.id.craft_bt_set);

        colorNormal = set.getCurrentTextColor();

        craftablePb.setIndeterminate(true);

        setPickersBorders();
        mp.setOnValueChangedListener(this);
        ammo.setOnValueChangedListener(this);
        rat.setOnValueChangedListener(this);
        parts.setOnValueChangedListener(this);
        tries.setOnValueChangedListener(this);
        tries.setMinValue(1); tries.setMaxValue(999); tries.setValue(1);
        setCraft("430/430/430/230");

        int idPickerInput = Resources.getSystem().getIdentifier("numberpicker_input", "id", "android");
        ((TextView)mp.findViewById(idPickerInput)).setImeOptions(EditorInfo.IME_ACTION_NEXT);
        ((TextView)ammo.findViewById(idPickerInput)).setImeOptions(EditorInfo.IME_ACTION_NEXT);
        ((TextView)rat.findViewById(idPickerInput)).setImeOptions(EditorInfo.IME_ACTION_NEXT);
        ((TextView)parts.findViewById(idPickerInput)).setImeOptions(EditorInfo.IME_ACTION_DONE);

        craftType.setOnSeekBarChangeListener(this);

        calculate();

        if (getIntent().hasExtra("id"))
            prepareAndParseDoll(getIntent().getIntExtra("id", -1));
    }

    private void setCraft(String craft) {
        CraftRecipe craftRec = new CraftRecipe(craft);
        mp.setValue(craftRec.mp);
        ammo.setValue(craftRec.ammo);
        rat.setValue(craftRec.rat);
        parts.setValue(craftRec.parts);
    }

    @SuppressLint("DefaultLocale")
    private String getCraft() {
        return String.format("%d/%d/%d/%d", mp.getValue(), ammo.getValue(), rat.getValue(), parts.getValue());
    }

    @SuppressWarnings("ConstantConditions")
    private void calculate() {
        if (craftableCheckerThread != null && craftableCheckerThread.isAlive()) craftableCheckerThread.interrupt();
        craftableCheckerThread = new Thread(() -> {
            String[] str = Parser.getCraftbleTypes(
                    new CraftRecipe(mp.getValue(), ammo.getValue(), rat.getValue(), parts.getValue()),
                    craftType.getProgress(), CraftActivity.this);

            if (str == null || Thread.currentThread().isInterrupted()) return;
            runOnUiThread(() -> setCraftable(str));
        });
        craftablePb.setVisibility(View.VISIBLE);
        craftable.setVisibility(View.GONE);
        craftableCheckerThread.setPriority(7);
        craftableCheckerThread.start();

        int triesValue = tries.getValue();
        CraftRecipe costCraft = new CraftRecipe(
                mp.getValue() * triesValue,
                ammo.getValue() * triesValue,
                rat.getValue() * triesValue,
                parts.getValue() * triesValue);
        cost.setText(costCraft.toString());

        HashMap<Integer, HashMap<Integer, Float>> craftChancesArrays =
                new Gson().fromJson(new InputStreamReader(getResources().openRawResource(R.raw.droprates)),
                        new TypeToken<HashMap<Integer, HashMap<Integer, Float>>>(){}.getType());
        HashMap<Integer, Float> craftChancesArray = craftChancesArrays.get(craftType.getProgress());
        assert craftChancesArray != null;
        @SuppressLint("DefaultLocale") String chancesStr = String.format("%s<br/>" +
                "<font color=\"#%06X\">★2 - %s</font><br/>" +
                "<font color=\"#%06X\">★3 - %s</font><br/>" +
                "<font color=\"#%06X\">★4 - %s</font><br/>" +
                "<font color=\"#%06X\">★5 - %s</font><br/>",
                getString(R.string.craft_chances),
                (0xFFFFFF & mGetColor(R.color.rarity_common)), String.format("%.2f", oneProke(craftChancesArray.get(2), triesValue) * 100) + "%",
                (0xFFFFFF & mGetColor(R.color.rarity_rare)), String.format("%.2f", oneProke(craftChancesArray.get(3), triesValue) * 100) + "%",
                (0xFFFFFF & mGetColor(R.color.rarity_epic)), String.format("%.2f", oneProke(craftChancesArray.get(4), triesValue) * 100) + "%",
                (0xFFFFFF & mGetColor(R.color.rarity_legend)), String.format("%.2f", oneProke(craftChancesArray.get(5), triesValue) * 100) + "%"
        );
        chances.setText(Html.fromHtml(chancesStr));
    }

    private void setCraftable(String[] str) {
        if (str.length > 0) {
            StringBuilder builder = new StringBuilder(getString(R.string.craft_craftable));
            builder.append(" ");
            for (String s : str)
                builder.append(s).append(", ");
            craftable.setText(builder.toString().substring(0, builder.length()-2));
        } else {
            craftable.setText(Html.fromHtml("<font color=#DD0000>No types can be produced (WTF?)</font>"));
        }
        craftablePb.setVisibility(View.GONE);
        craftable.setVisibility(View.VISIBLE);
    }

    private int mGetColor(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getColor(id);
        else return ResourcesCompat.getColor(getResources(), id, null);
    }

    private float oneProke(float chance, int count) {
        if (count == 1) return chance;
        if (count < 1) return 0;
        return oneProke(2 * chance - chance * chance, --count);
    }


    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        calculate();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        craftTypeTW.setText(CONSTRUCTION_TYPES[progress]);
        setPickersBorders();
        calculate();
    }

    private void setPickersBorders() {
        if (craftType.getProgress() > 0) {
            mp.setMinValue(1000); ammo.setMinValue(1000);
            rat.setMinValue(1000); parts.setMinValue(1000);
            mp.setMaxValue(9999); ammo.setMaxValue(9999);
            rat.setMaxValue(9999); parts.setMaxValue(9999);
        } else {
            boolean jump = mp.getValue() > 999 || ammo.getValue() > 999
                    || rat.getValue() > 999 || parts.getValue() > 999;
            mp.setMinValue(30); ammo.setMinValue(30);
            rat.setMinValue(30); parts.setMinValue(30);
            mp.setMaxValue(999); ammo.setMaxValue(999);
            rat.setMaxValue(999); parts.setMaxValue(999);
            if (jump) setCraft("430/430/430/230");
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    public void gotoLib(View v) {
        Intent intent = new Intent(this, LibraryActivity.class);
        switch (v.getId()) {
            case R.id.craft_bt_get:
                intent.putExtra("filter_craft", getCraft());
                intent.putExtra("filter_craftType", craftType.getProgress());
                intent.putExtra("title", getString(R.string.craft_libtitle_droplist));
                startActivity(intent);
                break;
            case R.id.craft_bt_set:
                if (dollToParse == null) {
                    intent.putExtra("filter_buildable", true);
                    intent.putExtra("title", getString(R.string.craft_libtitle_select));
                    startActivityForResult(intent, REQUEST_SETDOLL);
                } else {
                    dollToParse = null;
                    invalidateDollState();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_SETDOLL:
                if (resultCode != RESULT_OK || data == null) return;

                int id = data.getIntExtra("id", -1);
                prepareAndParseDoll(id);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        option.setOnItemSelectedListener(null);
        new Thread(this::parseDoll).start();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    @SuppressLint("DefaultLocale")
    private void prepareAndParseDoll(int id) {
        setLoading(true);

        Thread parserThread = new Thread(() -> {
            try {
                dollToParse = Parser.getCachedList(this)
                        .getById(id);
                if (dollToParse == null) throw new Exception("Failed to get doll");

                runOnUiThread(() -> {
                    invalidateDollState();

                    dollName.setText(dollToParse.getName());
                    dollMeta.setText(Html.fromHtml(
                            String.format("No.%d <font color=#%06X>%s</font> %s",
                                    dollToParse.getId(),
                                    (0xFFFFFF & ResourcesCompat.getColor(getResources(), RARITY_TABLE_COLOR[dollToParse.getRarity()], null)),
                                    RARITY_TABLE[dollToParse.getRarity()],
                                    dollToParse.getType())));

                    RequestOptions options = new RequestOptions().override(Dimension.setDp(80, 80, this).pxX);
                    if (dollToParse.getThumb().getHost().contains("fws"))
                        options = new RequestOptions()
                                .override(Dimension.setDp(80, 160, this).pxX, Dimension.setDp(80, 160, this).pxY)
                                .transform(new GFTWTransform());
                    Glide.with(this)
                            .load(dollToParse.getThumb().toString())
                            .apply(options)
                            .into(dollThumb);
                    dollView.setVisibility(View.VISIBLE);
                });

                parseDoll();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dollToParse = null;
                    invalidateDollState();
                    setLoading(false);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
        parserThread.setPriority(Thread.MAX_PRIORITY);
        parserThread.start();
    }

    private void invalidateDollState() {
        boolean b = dollToParse == null;

        mp.setEnabled(b);
        ammo.setEnabled(b);
        rat.setEnabled(b);
        parts.setEnabled(b);
        craftType.setEnabled(b);
        dollView.setVisibility(b ? View.GONE : View.VISIBLE);

        set.setText(b ? R.string.craft_select : R.string.craft_remove);
        set.setTextColor(b ? colorNormal
                : ResourcesCompat.getColor(getResources(), android.R.color.holo_red_light, null));
    }

    @SuppressLint("DefaultLocale")
    private void parseDoll() {
        if (dollToParse == null) return;
        runOnUiThread(() -> {
            option.setOnItemSelectedListener(null);
            setLoading(true);
        });

        try {
            if (dollToParse.getCraftReqs() == null && dollToParse.getHeavyCraftReqs() == null
                    || dollToParse.getCraftTime().equals("Unbuildable"))
                throw new Exception("Doll is unbuildable");

            CraftRecipe recipe;
            int type;
            switch (option.getSelectedItemPosition()) {
                default:
                case OPTION_MINIMUM:
                    type = dollToParse.getCraftReqs() != null ? 0 : 1;
                    String str = dollToParse.getCraftReqs() != null
                            ? dollToParse.getCraftReqs() : dollToParse.getHeavyCraftReqs();
                    if (str.matches("SUM:\\d+")) str = String.format("%1$d/%1$d/%1$d/%1$d",
                            Integer.valueOf(str.replaceAll("SUM:", "")) / 4);
                    recipe = new CraftRecipe(str);
                    break;
                case OPTION_RECCOMEND:
                    Crafts crafts = Crafts.load(this);
                    String regular = crafts.general.get(dollToParse.getType());
                    String heavy = crafts.heavy.get(dollToParse.getType());
                    Crafts.ExcEntry excEntry = crafts.exceptions.get(dollToParse.getId());
                    if (regular == null && heavy == null && excEntry == null)
                        throw new Exception("WTF? Standard craft for " + dollToParse.getType() + " has not found!");
                    assert regular != null;
                    assert heavy != null;

                    if (excEntry != null) {
                        type = excEntry.craftType;
                        recipe = new CraftRecipe(excEntry.value);
                    } else if (dollToParse.getCraftReqs() != null &&
                            CraftRecipe.isDollCraftable(dollToParse, regular, 0)) {
                        type = 0;
                        recipe = new CraftRecipe(regular);
                    } else if (dollToParse.getHeavyCraftReqs() != null &&
                            CraftRecipe.isDollCraftable(dollToParse, heavy, 1)) {
                        type = 1;
                        recipe = new CraftRecipe(heavy);
                    } else if (dollToParse.getCraftReqs() != null){
                        type = 0;
                        recipe = new CraftRecipe(dollToParse.getCraftReqs());
                    } else if (dollToParse.getHeavyCraftReqs() != null){
                        type = 1;
                        recipe = new CraftRecipe(dollToParse.getHeavyCraftReqs());
                    } else throw new Exception("Not found common recipe for " + dollToParse);
                    break;
            }

            runOnUiThread(() -> {
                craftType.setProgress(type);
                setCraft(recipe.toString());
                option.setEnabled(true);
                calculate();
                setLoading(false);
                option.setOnItemSelectedListener(this);
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                dollToParse = null;
                invalidateDollState();
                setLoading(false);
            });
        }
    }

    private void setLoading(boolean b) {
        if ((loading.getVisibility() == View.VISIBLE) == b) return;
        if (System.currentTimeMillis() - lastAnim < ANIM_DELAY) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> setLoading(b));
                }
            }, ANIM_DELAY);
            return;
        }

        ViewGroup showing = b ? loading : content;
        ViewGroup hidding = b ? content : loading;

        hidding.clearAnimation();
        showing.clearAnimation();
        hidding.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadeout));
        showing.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein));

        showing.setVisibility(View.VISIBLE);
        hidding.postOnAnimation(() -> {
            hidding.setVisibility(View.INVISIBLE);
            hidding.setAlpha(1);
        });

        lastAnim = System.currentTimeMillis();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra("request", requestCode);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
