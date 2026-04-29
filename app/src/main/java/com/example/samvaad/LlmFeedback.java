package com.example.samvaad;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * LlmFeedback — represents the coaching response from the AI analysis backend.
 * Firestore-compatible (no-arg constructor + getters).
 */
public class LlmFeedback implements Parcelable {

    public String summary;
    public List<String> strengths;
    public List<String> areasToImprove;
    public String behavioralProfile; 
    public String coachingTip;
    public int overallScore;
    public float paceScore;
    public float clarityScore;
    public float resilienceScore;
    public float presenceScore;
    public List<QuestionFeedback> questionAnalysis;

    public LlmFeedback() {
        strengths = new ArrayList<>();
        areasToImprove = new ArrayList<>();
        questionAnalysis = new ArrayList<>();
    }

    public LlmFeedback(String summary, List<String> strengths,
                        List<String> areasToImprove, String behavioralProfile,
                        String coachingTip, List<QuestionFeedback> questionAnalysis) {
        this.summary           = summary;
        this.strengths         = strengths;
        this.areasToImprove    = areasToImprove;
        this.behavioralProfile = behavioralProfile;
        this.coachingTip       = coachingTip;
        this.questionAnalysis  = questionAnalysis;
    }

    protected LlmFeedback(Parcel in) {
        summary = in.readString();
        strengths = in.createStringArrayList();
        areasToImprove = in.createStringArrayList();
        behavioralProfile = in.readString();
        coachingTip = in.readString();
        overallScore = in.readInt();
        paceScore = in.readFloat();
        clarityScore = in.readFloat();
        resilienceScore = in.readFloat();
        presenceScore = in.readFloat();
        questionAnalysis = in.createTypedArrayList(QuestionFeedback.CREATOR);
    }

    public static final Creator<LlmFeedback> CREATOR = new Creator<LlmFeedback>() {
        @Override
        public LlmFeedback createFromParcel(Parcel in) {
            return new LlmFeedback(in);
        }

        @Override
        public LlmFeedback[] newArray(int size) {
            return new LlmFeedback[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(summary);
        dest.writeStringList(strengths);
        dest.writeStringList(areasToImprove);
        dest.writeString(behavioralProfile);
        dest.writeString(coachingTip);
        dest.writeInt(overallScore);
        dest.writeFloat(paceScore);
        dest.writeFloat(clarityScore);
        dest.writeFloat(resilienceScore);
        dest.writeFloat(presenceScore);
        dest.writeTypedList(questionAnalysis);
    }

    // Standard getters (kept for source compatibility in existing Fragment code)
    public String getSummary()                { return summary; }
    public List<String> getStrengths()        { return strengths; }
    public List<String> getAreasToImprove()   { return areasToImprove; }
    public String getBehavioralProfile()      { return behavioralProfile; }
    public String getCoachingTip()            { return coachingTip; }
    public int getOverallScore()              { return overallScore; }
    public float getPaceScore()               { return paceScore; }
    public float getClarityScore()            { return clarityScore; }
    public float getResilienceScore()         { return resilienceScore; }
    public float getPresenceScore()           { return presenceScore; }
    public List<QuestionFeedback> getQuestionAnalysis() { return questionAnalysis; }

    public void setSummary(String summary)                       { this.summary = summary; }
    public void setStrengths(List<String> strengths)             { this.strengths = strengths; }
    public void setAreasToImprove(List<String> areasToImprove)   { this.areasToImprove = areasToImprove; }
    public void setBehavioralProfile(String behavioralProfile)   { this.behavioralProfile = behavioralProfile; }
    public void setCoachingTip(String coachingTip)               { this.coachingTip = coachingTip; }
    public void setOverallScore(int overallScore)                { this.overallScore = overallScore; }
    public void setPaceScore(float paceScore)                    { this.paceScore = paceScore; }
    public void setClarityScore(float clarityScore)              { this.clarityScore = clarityScore; }
    public void setResilienceScore(float resilienceScore)        { this.resilienceScore = resilienceScore; }
    public void setPresenceScore(float presenceScore)            { this.presenceScore = presenceScore; }
    public void setQuestionAnalysis(List<QuestionFeedback> qa)   { this.questionAnalysis = qa; }
}
