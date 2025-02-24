package com.tool.greeting_tool.ui.user.History;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tool.greeting_tool.R;
import com.tool.greeting_tool.common.constant.ErrorMessage;
import com.tool.greeting_tool.common.constant.MessageConstant;
import com.tool.greeting_tool.common.constant.TAGConstant;
import com.tool.greeting_tool.common.constant.URLConstant;
import com.tool.greeting_tool.common.utils.SharedPreferencesUtil;
import com.tool.greeting_tool.pojo.dto.GreetingCard;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The new version of history
 * show a dialog within user frgment
 * @noinspection ALL
 */
public class HistoryDialogFragment extends DialogFragment {

    private MessageAdapter messageAdapter;
    private ProgressBar progressBar;
    private List<History_Message> MessageList;

    public static HistoryDialogFragment newInstance() {
        return new HistoryDialogFragment();
    }

    /**
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     * or null if this is a freshly created Fragment.
     *
     * @return Dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_history, null);

        @SuppressLint("InflateParams") View customTitleView = inflater.inflate(R.layout.custom_dialog_title, null);

        Long currentUserId = SharedPreferencesUtil.getLong(requireContext());
        ListView listView = view.findViewById(R.id.history);
        progressBar = view.findViewById(R.id.progressBar_history);
        MessageList = new ArrayList<>();

        messageAdapter = new MessageAdapter(getActivity(), MessageList);
        listView.setAdapter(messageAdapter);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            History_Message message = messageAdapter.getItem(position);
            if (message != null) {
                Long deletedId = message.getCardId();
                showDeleteDialog(message, deletedId);
                //deleteMessage(deletedId);
            }
        });

        getGreetingCards(currentUserId);

        builder.setCustomTitle(customTitleView)
                .setView(view)
                .setNegativeButton("Close", null);

       AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(getResources().getColor(R.color.icon_color));
        });

        return dialog;
    }

    /**
     *
     * @param message : the message show to user
     */

    private void showDeleteDialog(History_Message message, long deletedId) {
        LayoutInflater inflater = getLayoutInflater();
        View titleView = inflater.inflate(R.layout.custom_dialog_title, null);
        View dialogView = inflater.inflate(R.layout.custom_dialog, null);

        TextView titleTextView = titleView.findViewById(R.id.custom_title);
        TextView messageTextView = dialogView.findViewById(R.id.dialog_message);

        titleTextView.setText(MessageConstant.Delete);
        messageTextView.setText(MessageConstant.DeleteMessage);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCustomTitle(titleView)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialogInterface, which) -> {
                    MessageList.remove(message);
                    messageAdapter.notifyDataSetChanged();
                    deleteMessage(deletedId);
                    //Toast.makeText(getActivity(), MessageConstant.Delete, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.no, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            positiveButton.setTextColor(getResources().getColor(R.color.icon_color));
            negativeButton.setTextColor(getResources().getColor(R.color.button_red));
        });

        dialog.show();
    }

    /**
     * Use to help user to check their history sent card
     * @param userId : the userID
     */

    private void getGreetingCards(Long userId) {
        progressBar.setVisibility(View.VISIBLE);
        String jwtToken = SharedPreferencesUtil.getToken(getActivity());

        // Create empty JSON body
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        String json = gson.toJson(jsonObject);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(URLConstant.GET_HISTORY_CARD_URL + "/" + userId)
                .header("Token", jwtToken)
                .post(body)
                .build();

        //send request
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d(TAGConstant.HISTORY_CARD_TAG,"Response: "+ responseBody);
                getActivity().runOnUiThread(() -> {
                    try {
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        int code = jsonResponse.get("code").getAsInt();
                        if (code == 1) {
                            ArrayList<GreetingCard> greetingCards = new Gson().fromJson(
                                    jsonResponse.get("data").getAsJsonArray(),
                                    new TypeToken<ArrayList<GreetingCard>>(){}.getType()
                            );
                            updateMessageList(greetingCards);
                        } else {
                            String msg = jsonResponse.get("msg").getAsString();
                            Toast.makeText(getActivity(), "Error: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAGConstant.HISTORY_CARD_TAG, "Exception while parsing response", e);
                        Toast.makeText(getActivity(), "Invalid response", Toast.LENGTH_SHORT).show();
                    }finally{
                        progressBar.setVisibility(View.GONE);
                    }
                });



            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAGConstant.HISTORY_CARD_TAG,"Get card failed",e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), "Get history card failed " + ErrorMessage.NETWORK_ERROR, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

    }
    private void updateMessageList(ArrayList<GreetingCard> greetingCards) {
        MessageList.clear();
        int id = 0;
        for (GreetingCard card : greetingCards) {
            MessageList.add(new History_Message(id++, card.getCardId(), card.getId(), card.getCreate_time()));
        }
        progressBar.setVisibility(View.GONE);
        messageAdapter.notifyDataSetChanged();
    }

    /**
     * user delete a greeting card
     * @param deletedId id need to be deleted
     */
    private void deleteMessage(Long deletedId) {
        String jwtToken = SharedPreferencesUtil.getToken(getActivity());

        Request request = new Request.Builder()
                .url(URLConstant.GET_DELETE_CARD_URL + "/" + deletedId)
                .header("Token", jwtToken)
                .delete()
                .build();

        //send request
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    // cancel success
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Delete card successfully", Toast.LENGTH_SHORT).show());
                } else {
                    //fail to cancel
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), ErrorMessage.INVALID_RESPONSE, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), ErrorMessage.NETWORK_ERROR, Toast.LENGTH_SHORT).show());
            }
        });


    }
}
