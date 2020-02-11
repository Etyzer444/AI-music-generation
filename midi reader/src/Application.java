import javax.sound.midi.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Application {
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static void main(String args[]) throws InvalidMidiDataException, IOException
    {
        File myFile=new File("can1.mid");
        Sequence mySequence;
        mySequence=MidiSystem.getSequence(myFile);
        Sequence outputSequence=new Sequence(mySequence.getDivisionType(),mySequence.getResolution(),1);
        FileWriter fw=new FileWriter("eventList.txt");;
        for(int i=0;i<mySequence.getTracks()[1].size();i++)
        {
            MidiEvent current=mySequence.getTracks()[1].get(i);
            fw.write("tick: "+ current.getTick() + " " + readMessage(current.getMessage())+"\n");
        }
        fw.flush();
        fw.close();
    }
    private static String readMessage(MidiMessage message)
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
                return "Note off: " + noteName + octave + " key: " + key + " velocity: " + velocity;
            }
            else if(sm.getCommand()==NOTE_ON)
            {
                int key = sm.getData1();
                int octave = (key / 12)-1;
                int note = key % 12;
                String noteName = NOTE_NAMES[note];
                int velocity = sm.getData2();
                return "Note on: " + noteName + octave + " key: " + key + " velocity: " + velocity;
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
