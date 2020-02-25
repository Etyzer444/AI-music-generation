import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import javax.sound.midi.InvalidMidiDataException;
import java.io.*;

public class testRNN {
    private static final int HIDDEN_LAYER_WIDTH = 50;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B","NOT A NOTE"};
    public static void main(String args[]) throws IOException, InvalidMidiDataException
    {
        DataExtractor de = new DataExtractor("dataset");
        de.readFile(new File("dataset/merge_from_ofoct.mid"));
        File data = new File("eventList.txt");
        int eventAmount= countLinesInAFile(data);

        GravesLSTM.Builder lstmBuilder = new GravesLSTM.Builder();
        lstmBuilder.activation(Activation.TANH.name());
        lstmBuilder.nIn(NOTE_NAMES.length);
        lstmBuilder.nOut(HIDDEN_LAYER_WIDTH); // Hidden
        GravesLSTM inputLayer = lstmBuilder.build();

        RnnOutputLayer.Builder outputBuilder = new RnnOutputLayer.Builder();
        outputBuilder.lossFunction(LossFunctions.LossFunction.MSE);
        outputBuilder.activation(Activation.SOFTMAX.name());
        outputBuilder.nIn(HIDDEN_LAYER_WIDTH); // Hidden
        outputBuilder.nOut(NOTE_NAMES.length);
        RnnOutputLayer outputLayer = outputBuilder.build();

        NeuralNetConfiguration.Builder nnBuilder = new NeuralNetConfiguration.Builder();
        nnBuilder.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT);
        nnBuilder.updater(Updater.ADAM);
        nnBuilder.weightInit(WeightInit.XAVIER);
        nnBuilder.learningRate(0.01);
        nnBuilder.miniBatch(true);

        MultiLayerNetwork network = new MultiLayerNetwork(
                nnBuilder.list().layer(0, inputLayer)
                        .layer(1, outputLayer)
                        .backprop(true).pretrain(false)
                        .build());

        network.init();

        INDArray inputArray = Nd4j.zeros(1, inputLayer.getNIn(), eventAmount);
        INDArray inputLabels = Nd4j.zeros(1, outputLayer.getNOut(), eventAmount);

        for(int i=0;i<eventAmount;i++) {
            int positionInValidCharacters1 = getNoteIndex(getNoteFromLine(data,i));
            inputArray.putScalar(new int[]{0, positionInValidCharacters1, i}, 1);

            int positionInValidCharacters2 = getNoteIndex(getNoteFromLine(data,i+1));
            inputLabels.putScalar(new int[]{0, positionInValidCharacters2, i}, 1);
        }
        DataSet dataSet = new DataSet(inputArray, inputLabels);

        for(int z=0;z<1000;z++) {
            network.fit(dataSet);

            INDArray testInputArray = Nd4j.zeros(inputLayer.getNIn());
            testInputArray.putScalar(0, 1);

            network.rnnClearPreviousState();
            String output = "";
            for (int k = 0; k < 200; k++) {
                INDArray outputArray = network.rnnTimeStep(testInputArray);
                double maxPrediction = Double.MIN_VALUE;
                int maxPredictionIndex = -1;
                for (int i = 0; i < NOTE_NAMES.length; i++) {
                    if (maxPrediction < outputArray.getDouble(i)) {
                        maxPrediction = outputArray.getDouble(i);
                        maxPredictionIndex = i;
                    }
                }
                // Concatenate generated character
                output += NOTE_NAMES[maxPredictionIndex];
                testInputArray = Nd4j.zeros(inputLayer.getNIn());
                testInputArray.putScalar(maxPredictionIndex, 1);
            }
            System.out.println(z + "> " + output + "\n----------\n");
        }


    }

    private static int countLinesInAFile(File f) throws IOException
    {
        FileReader fr= new FileReader(f);
        LineNumberReader count= new LineNumberReader(fr);
        while (count.read()!=-1)
        {
            count.readLine();
        }
        return count.getLineNumber();
    }
    private static int getNoteIndex(String note)
    {
        for(int i=0;i<NOTE_NAMES.length;i++)
        {
            if (note.equals(NOTE_NAMES[i]))
                return i;
        }
        return 12;
    }
    private static String getNoteFromLine(File f, int line) throws FileNotFoundException, IOException
    {
        FileInputStream fis= new FileInputStream(f);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        for(int i=0;i<line-1;i++)
        {
            br.readLine();
        }
        String myLine= br.readLine();
        for(int i=0;i<NOTE_NAMES.length;i++)
        {
            if(myLine.contains(NOTE_NAMES[i]))
            {
                return NOTE_NAMES[i];
            }
        }
        return NOTE_NAMES[12];

    }

}

