package com.clock.pt1.keeptesting.monkey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.clock.pt1.keeptesting.R;
import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public class PackageListActivity extends Activity {
	
	public static final int RETURN_CODE_OK_BUTTON = 0;
	public static final int RETURN_CODE_BACK_KEY = 1;
	
	private ListView packageListView;
	private Button okButton;
    private ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
    private PackageListAdapter adapter;
    private int action = 0;
    private String packageList;
    private Set<String> packageSet = new HashSet<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		setContentView(R.layout.monkey_package_list_layout);
		
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		action = bundle.getInt("action");
		packageList = bundle.getString("list");

		String[] sArray = packageList.split(";");
		for(String p:sArray) {
			if(!p.isEmpty()) {
				packageSet.add(p);
			}
		}
		
		okButton = (Button) this.findViewById(R.id.monkey_package_list_ok_button);
		packageListView = (ListView) this.findViewById(R.id.monkey_pacakge_list);
		
        PackageManager pm = getPackageManager();
        List<PackageInfo> packs = pm.getInstalledPackages(0);
        for(PackageInfo pi:packs){  
        	if(action == MonkeyConfigActivity.WHITE_LIST_MINUS ||
        			action == MonkeyConfigActivity.BLACK_LIST_MINUS) {
        		//there is a package named "android" which means the system itself, skip this package
        		if(packageList.contains(pi.applicationInfo.packageName)&& !pi.applicationInfo.packageName.equals("android")) {
		            HashMap<String, Object> map = new HashMap<String, Object>();
		            map.put("icon", pi.applicationInfo.loadIcon(pm));
		            map.put("appName", pi.applicationInfo.loadLabel(pm));
		            map.put("packageName", pi.applicationInfo.packageName);
		            map.put("selected", "yes");
		            items.add(map); 
        		}
        	} else {
        		//there is a package named "android" which means the system itself, skip this package
        		if(!packageList.contains(pi.applicationInfo.packageName)&& !pi.applicationInfo.packageName.equals("android")) {
		            HashMap<String, Object> map = new HashMap<String, Object>();
		            map.put("icon", pi.applicationInfo.loadIcon(pm));
		            map.put("appName", pi.applicationInfo.loadLabel(pm));
		            map.put("packageName", pi.applicationInfo.packageName);
		            map.put("selected", "no");
		            items.add(map); 
        		}
        	}
        }
        
		okButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
	            Intent data=new Intent();  
				Bundle bundle = new Bundle();
				packageList = packageSetToString(packageSet);
				bundle.putString("list",packageList);
				data.putExtras(bundle);
	            setResult(RETURN_CODE_OK_BUTTON, data);   
	            finish();  
			}
		});
		
        adapter = new PackageListAdapter(this, items, R.layout.monkey_package_list_item,   
                new String[]{"icon", "appName", "packageName","selected"},  
                new int[]{R.id.package_icon, R.id.package_app_name, R.id.package_name, R.id.select_check_box});  
        
        packageListView.setAdapter(adapter);
	}
	
	@Override
	public void onBackPressed(){
        Intent data=new Intent();  
		Bundle bundle = new Bundle();
		packageList = packageSetToString(packageSet);
		bundle.putString("list",packageList);
		data.putExtras(bundle);
        setResult(RETURN_CODE_BACK_KEY, data); 
		super.onBackPressed();
	}
	
	private String packageSetToString(Set<String> packageSet) {
		String result = "";
		for(String p:packageSet) {
			result += p;
			result += ";";
		}

		if(result.isEmpty()) {
			return "";
		} else {
			return result.substring(0,result.length()-1);
		}
	}
	
	
	class PackageListAdapter extends SimpleAdapter  
	{  
	    private int[] appTo;  
	    private String[] appFrom;  
	    private ViewBinder appViewBinder;  
	    private List<? extends Map<String, ?>>  appData;  
	    private int appResource;  
	    private LayoutInflater appInflater;  
	    
	    public PackageListAdapter(Context context, List<? extends Map<String, ?>> data,  
	            int resource, String[] from, int[] to) {  
	        super(context, data, resource, from, to);  
	        appData = data;  
	        appResource = resource;  
	        appFrom = from;  
	        appTo = to;  
	        appInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
	    }  
	      
	    public View getView(int position, View convertView, ViewGroup parent){  
	        return createViewFromResource(position, convertView, parent, appResource);  
	    }  
	      
	    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource){  
	        View v;  
	        
	        if(convertView == null){  
	            v = appInflater.inflate(resource, parent,false);  
	            final int[] to = appTo;  
	            final int count = to.length;  
	            final View[] holder = new View[count];  
	              
	            for(int i = 0; i < count; i++){  
	                holder[i] = v.findViewById(to[i]);  
	            }  
	            
	            v.setTag(holder);  
	        }else {  
	            v = convertView;  
	        }  
	        
	        bindView(position, v);  
	        return v;     
	    }  
	      
	    private void bindView(final int position, View view){  
	        final Map<String, ?> dataSet = appData.get(position);  

	        if(dataSet == null){  
	            return;  
	        }  
	          
	        final ViewBinder binder = appViewBinder;  
	        final View[] holder = (View[])view.getTag();  
	        final String[] from = appFrom;  
	        final int[] to = appTo;  
	        final int count = to.length;  
	          
	        for(int i = 0; i < count; i++){  
	            final View v = holder[i];  
	            
	            if(v != null){  
	                final Object data = dataSet.get(from[i]);  
	                String text = data == null ? "":data.toString();  
	                
	                if(text == null){  
	                    text = "";  
	                }  
	                  
	                boolean bound = false; 
	                
	                if(binder != null){  
	                    bound = binder.setViewValue(v, data, text);  
	                }  

					if (!bound) {

						if (v instanceof CheckBox) {
							((CheckBox) v)
									.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
										@Override
										public void onCheckedChanged(
												CompoundButton buttonView,
												boolean isChecked) {
											if (isChecked) {
												items.get(position).put(
														"selected", "yes");
												packageSet.add((String) items.get(position).get("packageName"));
											} else {
												items.get(position).put(
														"selected", "no");
												packageSet.remove((String) items.get(position).get("packageName"));
											}
										}
									});

							if (text.equals("yes")) {
								((CheckBox) v).setChecked(true);
							} else {
								((CheckBox) v).setChecked(false);
							}
						} else if (v instanceof TextView) {
							setViewText((TextView) v, text);
						} else if (v instanceof ImageView) {
							setViewImage((ImageView) v, (Drawable) data);
						} else {
							throw new IllegalStateException(
									v.getClass().getName()
											+ " is not a "
											+ "view that can be bounds by this SimpleAdapter");
						}
					}
				}
	        }  
	    }  
	    
	    public void setViewImage(ImageView v, Drawable value)  
	    {  
	        v.setImageDrawable(value);  
	    }  
	}
}


  