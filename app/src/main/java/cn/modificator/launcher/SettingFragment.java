package cn.modificator.launcher;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import cn.modificator.launcher.ftpservice.FTPService;
import cn.modificator.launcher.model.WifiControl;

/**
 * 设置页面 Fragment。
 */
public class SettingFragment extends Fragment implements View.OnClickListener {

  private Spinner colNumSpinner;
  private Spinner rowNumSpinner;
  private Spinner appNameLinesSpinner;
  private SeekBar fontControl;
  private View rootView;
  private TextView hideDivider;
  private TextView ftpAddr;
  private TextView ftpStatus;
  private TextView showStatusBar;
  private TextView showCustomIcon;
  private Config config;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_setting, null);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    rootView = getView();
    config = new Config(getActivity());
    initViews();
    initSpinners();
    initFontControl();
    updateFtpStatus();
  }

  // =========================================================================
  // 初始化
  // =========================================================================

  private void initViews() {
    rootView.findViewById(R.id.toBack).setOnClickListener(this);
    rootView.findViewById(R.id.rootView).setOnClickListener(this);
    rootView.findViewById(R.id.deleteApp).setOnClickListener(this);
    rootView.findViewById(R.id.showWifiName).setOnClickListener(this);
    rootView.findViewById(R.id.btnHideFontControl).setOnClickListener(this);
    rootView.findViewById(R.id.changeFontSize).setOnClickListener(this);
    rootView.findViewById(R.id.helpAbout).setOnClickListener(this);
    rootView.findViewById(R.id.menu_ftp).setOnClickListener(this);
    rootView.findViewById(R.id.openDeviceManager).setOnClickListener(this);

    showStatusBar = rootView.findViewById(R.id.showStatusBar);
    showCustomIcon = rootView.findViewById(R.id.showCustomIcon);
    ftpStatus = rootView.findViewById(R.id.ftp_status);
    ftpAddr = rootView.findViewById(R.id.ftp_addr);
    hideDivider = rootView.findViewById(R.id.hideDivider);
    fontControl = rootView.findViewById(R.id.font_control);
    colNumSpinner = rootView.findViewById(R.id.col_num_spinner);
    rowNumSpinner = rootView.findViewById(R.id.row_num_spinner);
    appNameLinesSpinner = rootView.findViewById(R.id.appNameLine);

    showStatusBar.setOnClickListener(this);
    hideDivider.setOnClickListener(this);
    showCustomIcon.setOnClickListener(this);

    // 初始化 UI 状态
    showStatusBar.getPaint().setStrikeThruText(config.isShowStatusBar());
    hideDivider.getPaint().setStrikeThruText(config.isHideDivider());
    hideDivider.setText(config.isHideDivider() ? "显示分隔线" : "隐藏分隔线");
    showCustomIcon.getPaint().setStrikeThruText(config.isShowCustomIcon());
    fontControl.setProgress((int) ((config.getFontSize() - 10) * 10));
  }

  private void initSpinners() {
    rowNumSpinner.setSelection(config.getRowNum() - 2, false);
    rowNumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int rowNum = position + 2;
        config.setRowNum(rowNum);
        sendLauncherUpdate(Config.KEY_ROW_NUM, rowNum);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    colNumSpinner.setSelection(config.getColNum() - 2, false);
    colNumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int colNum = position + 2;
        config.setColNum(colNum);
        sendLauncherUpdate(Config.KEY_COL_NUM, colNum);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    appNameLinesSpinner.setSelection(getAppLineSpinnerSelectPosition(), false);
    appNameLinesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        config.setAppNameLines(position);
        sendLauncherUpdate(Config.KEY_APP_NAME_LINES, position);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  private void initFontControl() {
    fontControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          float newSize = 10 + progress / 10f;
          config.setFontSize(newSize);
          Intent intent = new Intent(Launcher.ACTION_LAUNCHER_UPDATE);
          intent.putExtra(Config.KEY_FONT_SIZE, newSize);
          getActivity().sendBroadcast(intent);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });
  }

  private int getAppLineSpinnerSelectPosition() {
    int lines = config.getAppNameLines();
    return (lines < 3) ? lines : 3;
  }

  // =========================================================================
  // 广播辅助
  // =========================================================================

  private void sendLauncherUpdate(String key, int value) {
    Intent intent = new Intent(Launcher.ACTION_LAUNCHER_UPDATE);
    intent.putExtra(key, value);
    getActivity().sendBroadcast(intent);
  }

  private void sendLauncherUpdate(String key, boolean value) {
    Intent intent = new Intent(Launcher.ACTION_LAUNCHER_UPDATE);
    intent.putExtra(key, value);
    getActivity().sendBroadcast(intent);
  }

  // =========================================================================
  // 点击处理
  // =========================================================================

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.toBack || id == R.id.rootView) {
      getActivity().onBackPressed();
    } else if (id == R.id.deleteApp) {
      handleDeleteApp();
    } else if (id == R.id.showStatusBar) {
      handleToggleStatusBar();
    } else if (id == R.id.helpAbout) {
      AboutDialog.getInstance(getActivity()).show();
    } else if (id == R.id.btnHideFontControl) {
      rootView.findViewById(R.id.menuList).setVisibility(View.VISIBLE);
      rootView.findViewById(R.id.font_control_p).setVisibility(View.GONE);
    } else if (id == R.id.changeFontSize) {
      rootView.findViewById(R.id.menuList).setVisibility(View.GONE);
      rootView.findViewById(R.id.font_control_p).setVisibility(View.VISIBLE);
    } else if (id == R.id.hideDivider) {
      handleToggleDivider();
    } else if (id == R.id.menu_ftp) {
      handleFtp();
    } else if (id == R.id.showWifiName) {
      handleShowWifiName();
    } else if (id == R.id.showCustomIcon) {
      handleToggleCustomIcon();
    } else if (id == R.id.openDeviceManager) {
      startActivity(new Intent().setComponent(
          new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings")));
    }
  }

  private void handleDeleteApp() {
    sendLauncherUpdate(Config.KEY_HIDE_APPS, true);
    getActivity().onBackPressed();
  }

  private void handleToggleStatusBar() {
    boolean newValue = !config.isShowStatusBar();
    config.setShowStatusBar(newValue);
    sendLauncherUpdate(Config.KEY_SHOW_STATUS_BAR, newValue);
    getActivity().onBackPressed();
  }

  private void handleToggleDivider() {
    boolean newValue = !config.isHideDivider();
    config.setHideDivider(newValue);
    hideDivider.setText(newValue ? "显示分隔线" : "隐藏分隔线");
    sendLauncherUpdate(Config.KEY_HIDE_DIVIDER, newValue);
    getActivity().onBackPressed();
  }

  private void handleFtp() {
    Utils.checkStoragePermission(getActivity(), new Runnable() {
      @Override
      public void run() {
        if (!FTPService.isRunning()) {
          if (FTPService.isConnectedToWifi(getActivity())) {
            startFtpServer();
          } else {
            Toast.makeText(getActivity(), "大哥诶，麻烦先把WIFI连上吧", Toast.LENGTH_SHORT).show();
          }
        } else {
          stopFtpServer();
        }
      }
    });
  }

  private void handleShowWifiName() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10002);
    }
  }

  private void handleToggleCustomIcon() {
    Utils.checkStoragePermission(getActivity(), new Runnable() {
      @Override
      public void run() {
        boolean newValue = !config.isShowCustomIcon();
        config.setShowCustomIcon(newValue);
        sendLauncherUpdate(Config.KEY_SHOW_CUSTOM_ICON, newValue);
        getActivity().onBackPressed();
      }
    });
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 10002) {
      WifiControl.reloadWifiName();
      getActivity().onBackPressed();
    }
  }

  // =========================================================================
  // 生命周期
  // =========================================================================

  @Override
  public void onResume() {
    super.onResume();
    updateFtpStatus();

    IntentFilter wifiFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    Utils.registerReceiverCompat(getActivity(), wifiReceiver, wifiFilter);

    IntentFilter ftpFilter = new IntentFilter();
    ftpFilter.addAction(FTPService.ACTION_STARTED);
    ftpFilter.addAction(FTPService.ACTION_STOPPED);
    ftpFilter.addAction(FTPService.ACTION_FAILEDTOSTART);
    Utils.registerReceiverCompat(getActivity(), ftpReceiver, ftpFilter);
  }

  @Override
  public void onPause() {
    super.onPause();
    getActivity().unregisterReceiver(wifiReceiver);
    getActivity().unregisterReceiver(ftpReceiver);
  }

  // =========================================================================
  // FTP 控制
  // =========================================================================

  private void startFtpServer() {
    getActivity().sendBroadcast(new Intent(FTPService.ACTION_START_FTPSERVER));
  }

  private void stopFtpServer() {
    getActivity().sendBroadcast(new Intent(FTPService.ACTION_STOP_FTPSERVER));
  }

  private void updateFtpStatus() {
    if (FTPService.isConnectedToWifi(getActivity())) {
      if (FTPService.isRunning()) {
        ftpStatus.setText(R.string.setting_cloud_manager_on);
        ftpAddr.setVisibility(View.VISIBLE);
        String address = getFTPAddressString();
        if (address != null) {
          ftpAddr.setText(address);
        } else {
          ftpAddr.setVisibility(View.GONE);
        }
      } else {
        ftpStatus.setText(R.string.setting_cloud_manager_off);
        ftpAddr.setVisibility(View.GONE);
      }
    } else {
      ftpStatus.setText(R.string.setting_cloud_manager_wifi_off);
      ftpAddr.setVisibility(View.GONE);
    }
  }

  private String getFTPAddressString() {
    if (FTPService.getLocalInetAddress(getActivity()) == null) {
      return null;
    }
    return "ftp://" + FTPService.getLocalInetAddress(getActivity()).getHostAddress()
        + ":" + FTPService.getPort();
  }

  // =========================================================================
  // 广播接收器
  // =========================================================================

  private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netInfo = conMan.getActiveNetworkInfo();
      if (netInfo == null || netInfo.getType() != ConnectivityManager.TYPE_WIFI) {
        stopFtpServer();
      }
      updateFtpStatus();
    }
  };

  private final BroadcastReceiver ftpReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      updateFtpStatus();
    }
  };
}
