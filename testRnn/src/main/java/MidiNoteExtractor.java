import com.sun.jna.platform.win32.OaIdl;

import javax.sound.midi.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MidiNoteExtractor {
    String dataFolder;
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public MidiNoteExtractor(String df)
    {
        dataFolder=df;
    }
    public void readFile(File f) throws InvalidMidiDataException, IOException
    {
        Sequence mySequence= MidiSystem.getSequence(f);
        FileWriter fw=new FileWriter("f0.csv");
        FileWriter labels = new FileWriter("l0.csv");
        boolean[] notes= new boolean[]{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false};
        // 12 first fields in the array indicate whether note is ON when the given MidiEvent is happening, the last 12 indicate whether it is being uttered this event (to differentiate between holding a note and playing it repeatedly)
        int midiEventIterator=0;
        for(long i=0;i<mySequence.getTracks()[3].ticks();i++)
        {
            for(int j=12;j<24;j++)
            {
                notes[j]=false;
            }
            MidiEvent current=mySequence.getTracks()[3].get(midiEventIterator);
            while(current.getTick()==i)
            {
                notes=updateNoteArray(notes,current.getMessage());

                midiEventIterator++;
                current=mySequence.getTracks()[3].get(midiEventIterator);
            }
            fw.write(printNoteArray(notes));
            fw.write("\n");
            if(i>0)
            {
                labels.write(printNoteArray(notes)+"\n");
            }

        }
        for(int i=0;i<24;i++)
        {
            labels.write("0 ");//predict silence at the end of a track
        }
        labels.flush();
        labels.close();
        fw.flush();
        fw.close();

    }
    /*private String readMessage(MidiMessage message)
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

    }*/
    private boolean[] updateNoteArray(boolean[] previousState, MidiMessage message)
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
                previousState[note]=false;
                previousState[note+12]=false;
                return previousState;
            }
            else if(sm.getCommand()==NOTE_ON)
            {
                int key = sm.getData1();
                int octave = (key / 12)-1;
                int note = key % 12;
                String noteName = NOTE_NAMES[note];
                int velocity = sm.getData2();
                previousState[note]=true;
                previousState[note+12]=true;
                return previousState;
            }
            else
            {
                return previousState;
            }
        }
        else
        {
            return previousState;
        }
    }
    private String printNoteArray(boolean[] noteArray)
    {
        String output="";
        for(boolean i:noteArray)
        {
            if(i)
            {
                output=output+"1 ";
            }
            else
            {
                output=output+"0 ";
            }
        }
        return output;
    }

}
