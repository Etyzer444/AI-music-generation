import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.graph.vertex.impl.rnn.LastTimeStepVertex;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import javax.sound.midi.InvalidMidiDataException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static final int lstmLayerSize = 300;
    public static final int NB_EPOCHS = 1000;
    public static final int miniBatch=100;

    public static void main(String args[]) throws InvalidMidiDataException, IOException, InterruptedException {

        MidiNoteExtractor mne = new MidiNoteExtractor("data");
        mne.readFileShort(new File("data/test.mid"));
        int linesToSkip = 0;
        String delimiter = " ";
        CSVSequenceRecordReader feature = new CSVSequenceRecordReader(linesToSkip, delimiter);
        feature.initialize(new NumberedFileInputSplit("f%d.csv", 0, 0));
        CSVSequenceRecordReader label = new CSVSequenceRecordReader(linesToSkip, delimiter);
        label.initialize(new NumberedFileInputSplit("l%d.csv", 0, 0));
        DataSetIterator trainData = new SequenceRecordReaderDataSetIterator(feature, label, miniBatch, -1, true);

        // neural net
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new RmsProp(0.1))
                .seed(12345)
                .l2(0.001)
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new LSTM.Builder().nIn(trainData.inputColumns()).nOut(lstmLayerSize)
                        .activation(Activation.SOFTSIGN).build())
                .layer(1, new LSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                        .activation(Activation.SOFTSIGN).build())
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.IDENTITY)
                        .nIn(lstmLayerSize).nOut(trainData.inputColumns()).build())
                .backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(50).tBPTTBackwardLength(50)
                .build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        for(int i=0;i<NB_EPOCHS;i++)
        {
            System.out.println("epoch: "+ (i + 1));
            net.fit(trainData);
            feature.initialize(new NumberedFileInputSplit("f%d.csv", 0, 0));
            label.initialize(new NumberedFileInputSplit("l%d.csv", 0, 0));
            trainData = new SequenceRecordReaderDataSetIterator(feature, label, miniBatch, -1, true);
            if((i+1)%5==0) //create a sample every x epochs
            {
                FileWriter fw = new FileWriter((i + 1) + " epochsSample.csv");
                net.rnnClearPreviousState();
                int songLength = 20000;
                INDArray input = Nd4j.zeros(1, 5);
                for (int j = 0; j < songLength; j++)
                {
                    input = net.rnnTimeStep(input);
                    for (int k = 0; k < 5; k++) {
                        fw.write(input.getDouble(k) + " ");

                    }
                    fw.write("\n");

                }
                fw.flush();
                fw.close();
                System.out.println("wrote sample file for epoch"+ (i+1));
                mne.csvToMidi(new File((i+1) + " epochsSample.csv"),"song of "+(i+1)+ " epochs.mid");
            }

        }
    }
}
