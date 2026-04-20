package com.example.samvaad;

import android.os.Parcel;
import android.os.Parcelable;

public class QuestionFeedback implements Parcelable {
    public String question;
    public String whatYouSaidSummary;
    public String betterApproach;

    public QuestionFeedback() {}

    public QuestionFeedback(String question, String whatYouSaidSummary, String betterApproach) {
        this.question = question;
        this.whatYouSaidSummary = whatYouSaidSummary;
        this.betterApproach = betterApproach;
    }

    protected QuestionFeedback(Parcel in) {
        question = in.readString();
        whatYouSaidSummary = in.readString();
        betterApproach = in.readString();
    }

    public static final Creator<QuestionFeedback> CREATOR = new Creator<QuestionFeedback>() {
        @Override
        public QuestionFeedback createFromParcel(Parcel in) {
            return new QuestionFeedback(in);
        }

        @Override
        public QuestionFeedback[] newArray(int size) {
            return new QuestionFeedback[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(question);
        dest.writeString(whatYouSaidSummary);
        dest.writeString(betterApproach);
    }

    public String getQuestion() { return question; }
    public String getWhatYouSaidSummary() { return whatYouSaidSummary; }
    public String getBetterApproach() { return betterApproach; }

    public void setQuestion(String question) { this.question = question; }
    public void setWhatYouSaidSummary(String whatYouSaidSummary) { this.whatYouSaidSummary = whatYouSaidSummary; }
    public void setBetterApproach(String betterApproach) { this.betterApproach = betterApproach; }
}
