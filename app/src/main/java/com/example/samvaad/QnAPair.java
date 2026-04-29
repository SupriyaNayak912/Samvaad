package com.example.samvaad;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a single Q&A exchange during the interview session.
 * Stores the question asked by the AI, and the spoken transcript of the user's answer.
 */
public class QnAPair implements Parcelable {
    public String question;
    public String answerTranscript;

    // Required no-arg constructor for Firestore
    public QnAPair() {}

    public QnAPair(String question, String answerTranscript) {
        this.question = question;
        this.answerTranscript = answerTranscript;
    }

    protected QnAPair(Parcel in) {
        question = in.readString();
        answerTranscript = in.readString();
    }

    public static final Creator<QnAPair> CREATOR = new Creator<QnAPair>() {
        @Override
        public QnAPair createFromParcel(Parcel in) {
            return new QnAPair(in);
        }

        @Override
        public QnAPair[] newArray(int size) {
            return new QnAPair[size];
        }
    };

    public String getQuestion() { return question; }
    public String getAnswerTranscript() { return answerTranscript; }

    public void setQuestion(String question) { this.question = question; }
    public void setAnswerTranscript(String answerTranscript) { this.answerTranscript = answerTranscript; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(question);
        dest.writeString(answerTranscript);
    }
}
