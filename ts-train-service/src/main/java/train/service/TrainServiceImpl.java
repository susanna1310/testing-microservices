package train.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import train.entity.TrainType;
import train.repository.TrainTypeRepository;

@Service
public class TrainServiceImpl implements TrainService
{
    @Autowired
    private TrainTypeRepository repository;

    @Override
    public boolean create(TrainType trainType, HttpHeaders headers)
    {
        boolean result = false;
        // CHANGED so that object can be saved
        if (!repository.findById(trainType.getId()).isPresent()) {
            TrainType type = new TrainType(trainType.getId(), trainType.getEconomyClass(), trainType.getConfortClass());
            type.setAverageSpeed(trainType.getAverageSpeed());
            repository.save(type);
            result = true;
        }
        return result;
    }

    @Override
    public TrainType retrieve(String id, HttpHeaders headers)
    {
        if (!repository.findById(id).isPresent()) {
            return null;
        } else {
            return repository.findById(id).get();
        }
    }

    @Override
    public boolean update(TrainType trainType, HttpHeaders headers)
    {
        boolean result = false;
        if (repository.findById(trainType.getId()).isPresent()) {
            TrainType type = new TrainType(trainType.getId(), trainType.getEconomyClass(), trainType.getConfortClass());
            type.setAverageSpeed(trainType.getAverageSpeed());
            repository.save(type);
            result = true;
        }
        return result;
    }

    @Override
    public boolean delete(String id, HttpHeaders headers)
    {
        boolean result = false;
        if (repository.findById(id) != null) {
            repository.deleteById(id);
            result = true;
        }
        return result;
    }

    @Override
    public List<TrainType> query(HttpHeaders headers)
    {
        return repository.findAll();
    }
}
