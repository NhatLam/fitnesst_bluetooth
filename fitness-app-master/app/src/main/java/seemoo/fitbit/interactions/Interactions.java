package seemoo.fitbit.interactions;

import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import seemoo.fitbit.R;
import seemoo.fitbit.fragments.MainFragment;
import seemoo.fitbit.activities.WorkActivity;
import seemoo.fitbit.miscellaneous.FitbitDevice;
import seemoo.fitbit.information.InformationList;
import seemoo.fitbit.commands.Commands;
import seemoo.fitbit.tasks.Tasks;

/**
 * Use this class to deal with Bluetooth interactions.
 */
public class Interactions {

    private final String TAG = this.getClass().getSimpleName();

    private MainFragment mainFragment;
    private Toast toast;
    private Commands commands;
    private BluetoothInteractionQueue mBluetoothInteractionQueue;

    private boolean authenticated = false;
    private boolean liveModeActive = false;
    private boolean accelReadoutActive = false;

    private String currentInteraction = "";
    private Tasks tasks = null;

    /**
     * Creates an intance of interactions.
     *
     * @param mainFragment      The current mainFragment.
     * @param toast         The toast. to send messages to the user.
     * @param commands      The instance to commands.
     */
    public Interactions(MainFragment mainFragment, Toast toast, Commands commands) {
        this.mainFragment = mainFragment;
        this.toast = toast;
        this.commands = commands;
        mBluetoothInteractionQueue = new BluetoothInteractionQueue(this, (WorkActivity) mainFragment.getActivity(), toast);
    }

    /**
     * Finishes the current interaction.
     *
     * @return The data returned by the interactions finish method.
     */
    public Object interactionFinished() {
        Object result = null;
        if (mBluetoothInteractionQueue.getFirstBluetoothInteraction() != null) {
            Log.e(TAG, "Interaction finished: " + mBluetoothInteractionQueue.getFirstBluetoothInteraction().TAG);
            if(!mBluetoothInteractionQueue.isBluetoothInteractionsEmpty()) {
                result = mBluetoothInteractionQueue.getFirstBluetoothInteraction().finish();
            }            mBluetoothInteractionQueue.interactionFinished();
            if (tasks == null) {
                tasks = mainFragment.getTasks();
            }
            if (getCurrentInteraction().equals(tasks.getCurrentInteractionsTaskName())) {
                tasks.taskFinished();
            }
        } else {
            Log.e(TAG, "Interaction finished: null");
        }
        return result;
    }

    /**
     * Executes the first interaction in interaction queue.
     *
     * @param value
     * @return The interact result of the first instruction in queue. Null if there is no first instruction.
     */
    public InformationList interact(byte[] value) {
        if (mBluetoothInteractionQueue.getFirstBluetoothInteraction() != null) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {

            }
            return mBluetoothInteractionQueue.getFirstBluetoothInteraction().interact(value);
        } else {
            return null;
        }
    }

    /**
     * Checks, if the current interaction is finished.
     *
     * @return True, if there is no current interaction or it is finished.
     */
    public boolean isFinished() {
        return mBluetoothInteractionQueue.getFirstBluetoothInteraction() == null || mBluetoothInteractionQueue.getFirstBluetoothInteraction().isFinished();
    }

    /**
     * Returns, whether live mode is currently active.
     *
     * @return True, if the live mode is active.
     */
    public boolean liveModeActive() {
        return liveModeActive;
    }

    /**
     * Returns, whether live mode reads accelerometer data or activity data
     *
     * @return True, if the live mode reads accelerometer data.
     */
    public boolean accelReadoutActive() {
        return accelReadoutActive;
    }

    /**
     * Returns, whether app is already authenticated to the device.
     *
     * @return True, if the app is already authenticated.
     */
    public boolean getAuthenticated() {
        return authenticated;
    }

    /**
     * Sets accelerometer readout active to the given value.
     *
     * @param value The value to set accelerometer readout active to.
     */
    public void setAccelReadoutActive(boolean value) {
        accelReadoutActive = value;
    }

    /**
     * Sets the value of authenticated.
     *
     * @param value The value to set authenticated to.
     */
    void setAuthenticated(boolean value) {
        authenticated = value;
    }

    /**
     * Sets live mode active to the given value.
     *
     * @param value The value to set live mode active to.
     */
    void setLiveModeActive(boolean value) {
        liveModeActive = value;
    }

    /**
     * Returns the current interaction name.
     *
     * @return The current interaction name.
     */
    public String getCurrentInteraction() {
        return currentInteraction;
    }

    /**
     * Sets the current interaction name to the given value.
     *
     * @param interaction The name to set the current interaction name to.
     */
    void setCurrentInteraction(String interaction) {
        currentInteraction = interaction;
    }

    /**
     * <===============================================================================================================>
     * <=================================================> Interactions: <=============================================>
     * <===============================================================================================================>
     */

    //Always put an EmptyInteraction at last, to check if the last regular interaction in the interaction queue is working correctly.

    /**
     * Sets the instructions in the instruction queue, to establish an airlink with the device.
     */
    public void intEstablishAirlink() {
        mBluetoothInteractionQueue.addInteraction(new AirlinkInteraction(commands));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    public void letDeviceBlink(){
        mBluetoothInteractionQueue.addInteraction(new LedBlinkInteraction(commands));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to get a microdump from the device.
     */
    public void intMicrodump() {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new DumpInteraction(mainFragment, toast, commands, 0));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to get a megadump from the device.
     */
    public void intMegadump() {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new DumpInteraction(mainFragment, toast, commands, 1));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to get the alarms from the device. If the device name is 'Alta', this instruction gets not supported.
     */
    public void intGetAlarm() {
        if (commands.getmBluetoothGatt().getDevice().getAddress().equals(mainFragment.getString(R.string.alta))) {
            mainFragment.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast.setText("GetAlarm is not supported by this device!");
                    toast.show();
                }
            });
            Log.e(TAG, "GetAlarm is not supported by this device!");
        } else {
            intEstablishAirlink();
            mBluetoothInteractionQueue.addInteraction(new DumpInteraction(mainFragment, toast, commands, 2));
            mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
        }
    }

    /**
     * Sets the instructions in the instruction queue, to readout a part of the memory.
     *
     * @param addressBegin The start address of the memory part.
     * @param addressEnd   The end address of the memory part
     * @param memoryName   The name of the memory part. (Needed for later identification)
     */
    public void intReadOutMemory(String addressBegin, String addressEnd, String memoryName) {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new DumpInteraction(mainFragment, toast, commands, 3, addressBegin, addressEnd, memoryName));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to set the alarms of the device.
     *
     * @param position        The position of the alarm in the alarm list.
     * @param informationList The alarms.
     */
    public void intSetAlarm(int position, InformationList informationList) {
        if (commands.getmBluetoothGatt().getDevice().getAddress().equals(mainFragment.getString(R.string.alta))) {
            mainFragment.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast.setText("SetAlarm is not supported by this device!");
                    toast.show();
                }
            });
            Log.e(TAG, "SetAlarm is not supported by this device!");
        } else {
            intEstablishAirlink();
            mBluetoothInteractionQueue.addInteraction(new UploadInteraction(mainFragment, toast, commands, this, position, informationList));
            mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
        }
    }

    /**
     * Sets the instructions in the instruction queue, to clear the alarm lsit, which means to set all alarms to empty alarms.
     */
    public void intClearAlarms() {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new UploadInteraction(mainFragment, toast, commands, this, -1, new InformationList("")));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to upload a firmware onto the device.
     *
     * @param data         The data of the firmware to upload.
     * @param customLength The length of the data to upload. If it is set to a negative value, the length gets calculated.
     */
    public void intUploadFirmwareInteraction(String data, int customLength) {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new UploadInteraction(mainFragment, toast, commands, this, data, customLength));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to upload a microdump to the device.
     *
     * @param data The data of the alarm to upload.
     */
    public void intUploadMicroDumpInteraction(String data) {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new UploadInteraction(mainFragment, toast, commands, 1, data));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to upload a megadump to the device.
     *
     * @param data The data of the megadump to upload.
     */
    public void intUploadMegadumpInteraction(String data) {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new UploadInteraction(mainFragment, toast, commands, 2, data));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to authenticate to the device. Only, if the device is not already authenticated.
     * For this procedure, the serial number of the device is needed. If it is not set yet, it fetches a microdump from the device, to get it.
     */
    public void intAuthentication() {
        intEstablishAirlink();
        if (FitbitDevice.SERIAL_NUMBER == null) {
            mBluetoothInteractionQueue.addInteraction(new DumpInteraction(mainFragment, toast, commands, 0));
        }
        if (!authenticated) {
            mBluetoothInteractionQueue.addInteraction(new AuthenticationInteraction(mainFragment, toast, commands, this));

            String nonce = FitbitDevice.NONCE;
            String key = FitbitDevice.AUTHENTICATION_KEY;

            if (FitbitDevice.NONCE == null) {

                mBluetoothInteractionQueue.addInteraction(new AuthenticationInteraction(mainFragment, toast, commands, this));
            }
        } else {
            mainFragment.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast.setText("Already authenticated.");
                    toast.show();
                }
            });
            Log.e(TAG, "Already authenticated.");
        }
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to switch to live mode.
     *
     */
    public void intLiveModeEnable() {
        mBluetoothInteractionQueue.addInteraction(new LiveModeInteraction(commands, this, 1));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));

        setAccelReadoutActive(commands.isLiveModeAccelReadout());
    }

    /**
     * Sets the instructions in the instruction queue, to quit live mode.
     *
     */
    public void intLiveModeDisable() {
        interactionFinished();
        liveModeActive = false;
    }

    /**
     * Sets the instructions in the instruction queue, to set the date of the device.
     */
    public void intSetDate() {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new SetDateInteraction(mainFragment, toast, commands));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Sets the instructions in the instruction queue, to an empty interaction, which does nothing.
     */
    public void intEmptyInteraction() {
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Reads information from the console.
     */
    public void intConsolePrintf() {
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    /**
     * Turns on Accelormeter Readout. Instead of live-mode data the raw accelerometer data gets transmitted
     */
    public void intAccelReadout() {
        intEstablishAirlink();
        mBluetoothInteractionQueue.addInteraction(new LiveModeInteraction(commands, this, 0));
        mBluetoothInteractionQueue.addInteraction(new EmptyInteraction(this));
    }

    public void disconnectBluetooth() {
        mBluetoothInteractionQueue.addInteraction(new DisconnectBluetoothInteraction(mainFragment, commands));
    }
}
