import javax.sound.midi.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
public class DataExtractor {
    String dataFolder;
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public DataExtractor(String df)
    {
        dataFolder=df;
    }
    public void readFile(File f) throws InvalidMidiDataException, IOException
    {
        Sequence mySequence=MidiSystem.getSequence(f);
        FileWriter fw=new FileWriter("eventList.txt");
        long tickOfPreviousEvent=0;
        for(int i=0;i<mySequence.getTracks()[3].size();i++)
        {
            MidiEvent current=mySequence.getTracks()[3].get(i);
            fw.write( current.getTick()-tickOfPreviousEvent + " " + readMessage(current.getMessage())+"\n");
            tickOfPreviousEvent=current.getTick();
        }
        fw.flush();
        fw.close();

    }
    private String readMessage(MidiMessage message)
    {
        if(message instanceof ShortMessage)
        {
            ShortMessage sm=(ShortMessage)message;
            if(sm.getCommand()==NOTE_OFF || (sm.getCommand()==NOTE_ON && sm.getData2()==0))
            {
                int key = sm.getData1();
                int octave = (key / 12)-1;
                int note = key % 12;
                String noteName = NOTE_NAMES[note];
                int velocity = sm.getData2();
                return "off " + noteName + " " + octave + " " + velocity;
            }
            else if(sm.getCommand()==NOTE_ON)
            {
                int key = sm.getData1();
                int octave = (key / 12)-1;
                int note = key % 12;
                String noteName = NOTE_NAMES[note];
                int velocity = sm.getData2();
                return "on " + noteName + " " + octave + " " + velocity;
            }
            else
            {
                return "Command: " + Integer.toString(sm.getCommand());
            }
        }
        else
        {
            return "Other message";
        }

    }
}
