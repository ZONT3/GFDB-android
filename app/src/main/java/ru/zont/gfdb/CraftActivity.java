package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Random;

import ru.zont.gfdb.core.CraftRec;

public class CraftActivity
        extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener, NumberPicker.OnValueChangeListener {
    private static final int[] RARITY_TABLE_COLOR = {-1, -1, R.color.rarity_common, R.color.rarity_rare,
            R.color.rarity_epic, R.color.rarity_legend, R.color.rarity_extra};
    private static final int[] CONSTRUCTION_TYPES = {
            R.string.craft_constype_normal, R.string.craft_constype_large1,
            R.string.craft_constype_large2, R.string.craft_constype_large3};

    private NumberPicker tries;
    private NumberPicker parts;
    private NumberPicker rat;
    private NumberPicker ammo;
    private NumberPicker mp;
    private SeekBar lc;
    private Button set;
    private Button get;
    private TextView constr;
    private TextView cost;
    private TextView chances;


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

        mp = findViewById(R.id.craft_mp);
        ammo = findViewById(R.id.craft_ammo);
        rat = findViewById(R.id.craft_rat);
        parts = findViewById(R.id.craft_parts);
        tries = findViewById(R.id.craft_tries);
        lc = findViewById(R.id.craft_lc);
        set = findViewById(R.id.craft_bt_set);
        get = findViewById(R.id.craft_bt_get);
        constr = findViewById(R.id.craft_lc_tw);
        cost = findViewById(R.id.craft_cost);
        chances = findViewById(R.id.craft_chances);

        mp.setMinValue(30); mp.setMaxValue(9999); mp.setOnValueChangedListener(this);
        ammo.setMinValue(30); ammo.setMaxValue(9999); ammo.setOnValueChangedListener(this);
        rat.setMinValue(30); rat.setMaxValue(9999); rat.setOnValueChangedListener(this);
        parts.setMinValue(30); parts.setMaxValue(9999); parts.setOnValueChangedListener(this);
        tries.setMinValue(1); tries.setMaxValue(999); tries.setValue(1); tries.setOnValueChangedListener(this);
        setCraft("400/400/400/200");
        
        lc.setOnSeekBarChangeListener(this);

        calculate();
    }

    void setCraft(String craft) {
        CraftRec craftRec = new CraftRec(craft);
        mp.setValue(craftRec.mp);
        ammo.setValue(craftRec.ammo);
        rat.setValue(craftRec.rat);
        parts.setValue(craftRec.parts);
    }

    @SuppressWarnings("ConstantConditions")
    private void calculate() {
        int triesValue = tries.getValue();
        CraftRec costCraft = new CraftRec(
                mp.getValue() * triesValue,
                ammo.getValue() * triesValue,
                rat.getValue() * triesValue,
                parts.getValue() * triesValue);
        cost.setText(costCraft.toString());

        HashMap<Integer, HashMap<Integer, Float>> craftChancesArrays =
                new Gson().fromJson(new InputStreamReader(getResources().openRawResource(R.raw.droprates)),
                        new TypeToken<HashMap<Integer, HashMap<Integer, Float>>>(){}.getType());
        HashMap<Integer, Float> craftChancesArray = craftChancesArrays.get(lc.getProgress());
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
        constr.setText(CONSTRUCTION_TYPES[progress]);
        calculate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }
}
