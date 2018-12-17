package cn.jr1.msghook;

import java.util.ArrayList;

public class OcrBean {

    private String log_id;

    private String words_result_num;

    private ArrayList<Word> words_result;

    public String getLog_id() {
        return log_id;
    }

    public void setLog_id(String log_id) {
        this.log_id = log_id;
    }

    public String getWords_result_num() {
        return words_result_num;
    }

    public void setWords_result_num(String words_result_num) {
        this.words_result_num = words_result_num;
    }

    public ArrayList<Word> getWords_result() {
        return words_result;
    }

    public void setWords_result(ArrayList<Word> words_result) {
        this.words_result = words_result;
    }

    public  class Word{

        private String words;

        public String getWords() {
            return words;
        }

        public void setWords(String words) {
            this.words = words;
        }
    }




}


