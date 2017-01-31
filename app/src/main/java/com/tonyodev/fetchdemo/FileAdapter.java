package com.tonyodev.fetchdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tonyofrancis on 1/24/17.
 */

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> implements FetchListener {

    private final List<Download> downloads = new ArrayList<>();
    private final ActionListener actionListener;

    public FileAdapter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public FileAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.download_item,parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileAdapter.ViewHolder holder, int position) {

        Download download = downloads.get(position);

        Uri uri = Uri.parse(download.getUrl());
        holder.titleTextView.setText(uri.getLastPathSegment());
        holder.statusTextView.setText(getStatusString(download.getStatus()));
        holder.progressBar.setProgress(download.getProgress());

        onBindViewHolderActions(holder,position);

        switch (download.getStatus()) {
            case Fetch.STATUS_DONE: {
                holder.showViewButton();
                break;
            }
            case Fetch.STATUS_ERROR: {
                holder.showRetryButton();
                break;
            }
            case Fetch.STATUS_PAUSED: {
                holder.showPauseButton(R.string.resume);
                break;
            }
            case Fetch.STATUS_DOWNLOADING:
            case Fetch.STATUS_QUEUED: {
                holder.showPauseButton(R.string.pause);
                break;
            }
            default: {
                break;
            }
        }
    }

    private void onBindViewHolderActions(final FileAdapter.ViewHolder holder, int position) {

        final Download download = downloads.get(position);
        final int status = download.getStatus();
        final Context context = holder.itemView.getContext();

        //Set Pause and resume action
        holder.pauseResumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(status == Fetch.STATUS_PAUSED) {
                    actionListener.onResumeDownload(download.getId());

                }else if(status == Fetch.STATUS_DOWNLOADING || status == Fetch.STATUS_QUEUED) {
                    actionListener.onPauseDownload(download.getId());
                }
            }
        });

        //Set delete action
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Uri uri = Uri.parse(download.getUrl());

                new AlertDialog.Builder(context)
                        .setMessage(context.getString(R.string.delete_title,uri.getLastPathSegment()))
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                actionListener.onRemoveDownload(download.getId());
                            }
                        })
                        .setNegativeButton(R.string.cancel,null)
                        .show();

                return true;
            }
        });

        //Set View media listener
        holder.viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(status == Fetch.STATUS_DONE) {

                    File file = new File(download.getFilePath());
                    Uri uri = Uri.fromFile(file);

                    Intent share = new Intent(Intent.ACTION_VIEW);
                    share.setDataAndType(uri,Utils.getMimeType(context,uri));

                    context.startActivity(share);
                }
            }
        });

        //Set retry action
        holder.retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionListener.onRetryDownload(download.getId());
            }
        });
    }

    public void addDownload(Download download) {

        if (download != null) {

            downloads.add(download);
            notifyItemInserted(downloads.size() - 1);
        }
    }

    public Download getDownload(long id) {

        for (Download download : downloads) {

            if(download.getId() == id) {
                return download;
            }
        }

        return null;
    }

    public int getPosition(long id) {

        for (int i = 0; i < downloads.size(); i++) {

            Download download = downloads.get(i);

            if(download.getId() == id) {
                return i;
            }
        }
        return  -1;
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    @Override
    public void onUpdate(long id, int status, int progress, int error) {

        Download download = getDownload(id);

        if(download != null) {

            int position = getPosition(id);

            if(status != Fetch.STATUS_REMOVED) {

                download.setStatus(status);
                download.setProgress(progress);
                download.setError(error);

                notifyItemChanged(position);

            }else {
                downloads.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public String getStatusString(int status) {

        switch (status) {
            case Fetch.STATUS_DONE:
                return "Done";
            case Fetch.STATUS_DOWNLOADING:
                return "Downloading";
            case Fetch.STATUS_ERROR:
                return "Error";
            case Fetch.STATUS_PAUSED:
                return "Paused";
            case Fetch.STATUS_QUEUED:
                return "Waiting in Queue";
            case Fetch.STATUS_REMOVED:
                return "Removed";
            case Fetch.STATUS_NOT_QUEUED:
                return "Not Queued";
            default:
                return "Unknown";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView titleTextView;
        final TextView statusTextView;
        final ProgressBar progressBar;
        final Button pauseResumeButton;
        final Button viewButton;
        final Button retryButton;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = (TextView) itemView.findViewById(R.id.titleTextView);
            statusTextView = (TextView) itemView.findViewById(R.id.status_TextView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            pauseResumeButton = (Button) itemView.findViewById(R.id.pauseResume);
            viewButton = (Button) itemView.findViewById(R.id.viewButton);
            retryButton = (Button) itemView.findViewById(R.id.retryButton);
        }

        public void showViewButton() {
            pauseResumeButton.setVisibility(View.GONE);
            viewButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
        }

        public void showPauseButton(@StringRes int id) {
            pauseResumeButton.setText(id);
            pauseResumeButton.setVisibility(View.VISIBLE);
            viewButton.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);
        }

        public void showRetryButton() {
            pauseResumeButton.setVisibility(View.GONE);
            viewButton.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);
        }
    }
}
