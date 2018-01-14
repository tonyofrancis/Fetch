package com.tonyodev.fetchapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final List<DownloadData> downloads = new ArrayList<>();
    private final ActionListener actionListener;

    public FileAdapter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.download_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.actionButton.setOnClickListener(null);
        holder.actionButton.setEnabled(true);

        final DownloadData downloadData = downloads.get(position);
        final Uri uri = Uri.parse(downloadData.download.getUrl());
        final Status status = downloadData.download.getStatus();
        final Context context = holder.itemView.getContext();

        holder.titleTextView.setText(uri.getLastPathSegment());
        holder.statusTextView.setText(getStatusString(status));

        int progress = downloadData.download.getProgress();
        if (progress == -1) { // Download progress is undermined at the moment.
            progress = 0;
        }
        holder.progressBar.setProgress(progress);
        holder.progressTextView.setText(context.getString(R.string.percent_progress, progress));

        if (downloadData.eta == -1) {
            holder.timeRemainingTextView.setText("");
        } else {
            holder.timeRemainingTextView.setText(Utils.getETAString(context,
                    downloadData.eta));
        }

        if (downloadData.downloadedBytesPerSecond == 0) {
            holder.downloadedBytesPerSecondTextView.setText("");
        } else {
            holder.downloadedBytesPerSecondTextView.setText(Utils.getDownloadSpeedString(context,
                    downloadData.downloadedBytesPerSecond));
        }

        switch (status) {
            case COMPLETED: {
                holder.actionButton.setText(R.string.view);
                holder.actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Toast.makeText(context, "Downloaded Path:" +
                                    downloadData.download.getFile(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        final File file = new File(downloadData.download.getFile());
                        final Uri uri = Uri.fromFile(file);
                        final Intent share = new Intent(Intent.ACTION_VIEW);
                        share.setDataAndType(uri, Utils.getMimeType(context, uri));
                        context.startActivity(share);
                    }
                });
                break;
            }
            case FAILED: {
                holder.actionButton.setText(R.string.retry);
                holder.actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        holder.actionButton.setEnabled(false);
                        actionListener.onRetryDownload(downloadData.download.getId());
                    }
                });
                break;
            }
            case PAUSED: {
                holder.actionButton.setText(R.string.resume);
                holder.actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        holder.actionButton.setEnabled(false);
                        actionListener.onResumeDownload(downloadData.download.getId());
                    }
                });
                break;
            }
            case DOWNLOADING:
            case QUEUED: {
                holder.actionButton.setText(R.string.pause);
                holder.actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        holder.actionButton.setEnabled(false);
                        actionListener.onPauseDownload(downloadData.download.getId());
                    }
                });
                break;
            }
            default: {
                break;
            }
        }

        //Set delete action
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final Uri uri = Uri.parse(downloadData.download.getUrl());
                new AlertDialog.Builder(context)
                        .setMessage(context.getString(R.string.delete_title, uri.getLastPathSegment()))
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                actionListener.onRemoveDownload(downloadData.download.getId());
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();

                return true;
            }
        });

    }

    public void addDownload(final Download download) {
        if (download != null) {
            boolean found = false;
            for (DownloadData downloadData : downloads) {
                if (downloadData.id == download.getId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                final DownloadData downloadData = new DownloadData();
                downloadData.id = download.getId();
                downloadData.download = download;
                downloads.add(downloadData);
                notifyItemInserted(downloads.size() - 1);
            }

        }
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }


    public void update(final Download download, long eta, long downloadedBytesPerSecond) {
        if (download != null) {
            for (int position = 0; position < downloads.size(); position++) {
                final DownloadData downloadData = downloads.get(position);
                if (downloadData.id == download.getId()) {
                    switch (download.getStatus()) {
                        case REMOVED:
                        case DELETED: {
                            downloads.remove(position);
                            notifyItemRemoved(position);
                            break;
                        }
                        default: {
                            downloadData.download = download;
                            downloadData.eta = eta;
                            downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond;
                            notifyItemChanged(position);
                        }
                    }
                    return;
                }
            }
        }
    }

    public String getStatusString(Status status) {
        switch (status) {
            case COMPLETED:
                return "Done";
            case DOWNLOADING:
                return "Downloading";
            case FAILED:
                return "Error";
            case PAUSED:
                return "Paused";
            case QUEUED:
                return "Waiting in Queue";
            case REMOVED:
                return "Removed";
            case NONE:
                return "Not Queued";
            default:
                return "Unknown";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView titleTextView;
        public final TextView statusTextView;
        public final ProgressBar progressBar;
        public final TextView progressTextView;
        public final Button actionButton;
        public final TextView timeRemainingTextView;
        public final TextView downloadedBytesPerSecondTextView;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = (TextView) itemView.findViewById(R.id.titleTextView);
            statusTextView = (TextView) itemView.findViewById(R.id.status_TextView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            actionButton = (Button) itemView.findViewById(R.id.actionButton);
            progressTextView = (TextView) itemView.findViewById(R.id.progress_TextView);
            timeRemainingTextView = (TextView) itemView.findViewById(R.id.remaining_TextView);
            downloadedBytesPerSecondTextView = (TextView) itemView.findViewById(R.id.downloadSpeedTextView);
        }

    }

    public static class DownloadData {
        public int id;
        public Download download;
        public long eta = -1;
        public long downloadedBytesPerSecond = 0;

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return download.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof DownloadData) {
                return ((DownloadData) obj).id == id;
            }
            return false;
        }
    }

}
