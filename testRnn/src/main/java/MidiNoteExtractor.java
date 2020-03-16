import javax.sound.midi.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Scanner;

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
                //String noteName = NOTE_NAMES[note];
                int velocity = sm.getData2();
                return "-100 " + note + " " + octave + " " + velocity;
            }
            else if(sm.getCommand()==NOTE_ON)
            {
                int key = sm.getData1();
                int octave = (key / 12)-1;
                int note = key % 12;
                //String noteName = NOTE_NAMES[note];
                int velocity = sm.getData2();
                return "100 " + note + " " + octave + " " + velocity;
            }
            else
            {
                return "0 0 0 0";
            }
        }
        else
        {
            return "0 0 0 0";
        }

    }
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
    public void readFileShort(File f) throws IOException, InvalidMidiDataException
    {
        Sequence mySequence= MidiSystem.getSequence(f);
        FileWriter fw=new FileWriter("f0.csv");
        FileWriter labels = new FileWriter("l0.csv");
        long currentTick=0;
        for(int i=0;i<mySequence.getTracks()[3].size();i++)
        {

            MidiEvent current=mySequence.getTracks()[3].get(i);
            fw.write((current.getTick()-currentTick) +" "+ readMessage(current.getMessage()) + "\n");
            if(i>0)
            {
                labels.write((current.getTick()-currentTick) +" "+ readMessage(current.getMessage()) + "\n");
            }
            currentTick=current.getTick();
        }
        labels.write("0 0 0 0 0"); //predict nothing at the end of the song
        labels.flush();
        fw.flush();
        labels.close();
        fw.close();
    }
    public void csvToMidi(File f, String filename) throws FileNotFoundException, InvalidMidiDataException, IOException
    {
        Sequence newSong= new Sequence(Sequence.PPQ,24);
        int ticks=0;
        Track t=newSong.createTrack();
        Scanner scan = new Scanner(f);
        scan.useLocale(new Locale("en", "US"));
        while(scan.hasNextDouble())
        {
            int newTicks=tickCounter(scan.nextDouble());
            int onOff=onOffCounter(scan.nextDouble());
            int note=noteCounter(scan.nextDouble());
            int octave=octaveCounter(scan.nextDouble());
            int velocity=velocityCounter(scan.nextDouble());
            int data1=(octave*12)+note;
            if(onOff==100)
            {
                ShortMessage myMessage = new ShortMessage();
                myMessage.setMessage(NOTE_ON,data1,velocity);
                t.add(new MidiEvent(myMessage, ticks + newTicks));
            }
            else if (onOff==-100)
            {
                ShortMessage myMessage = new ShortMessage();
                myMessage.setMessage(NOTE_OFF,data1,velocity);
                t.add(new MidiEvent(myMessage, ticks + newTicks));
            }
            ticks+=newTicks;
        }
        File newFile=new File(filename);
        System.out.println(newSong.getMicrosecondLength());
        MidiSystem.write(newSong,1,newFile);


    }
    private int tickCounter(double val)
    {
        if(val<=0.5)
        {
            return 0;
        }
        else return (int) Math.round(val);
    }
    private int noteCounter(double val)
    {
        if(val<=0.5) return 0;
        else if(val>=11) return 11;
        else return (int) Math.round(val);
    }
    private int octaveCounter(double val)
    {
        if(val<=0.5) return 0;
        else if(val>=9) return 9;
        else return (int) Math.round(val);
    }
    private int velocityCounter(double val)
    {
        if(val<=0.5) return 0;
        else if(val>=127) return 127;
        else return (int) Math.round(val);
    }
    private int onOffCounter(double val)
    {
        if(val<=0) return -100;
        else return 100;
    }

}
