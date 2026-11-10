package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.ShelterStock;
import com.rajesh.aashraynet.entity.ShelterStockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ShelterStockRepository extends JpaRepository<ShelterStock, ShelterStockId> {

    @Query("select st from ShelterStock st join fetch st.item where st.id.shelterId = :shelterId order by st.item.name")
    List<ShelterStock> findForShelter(@Param("shelterId") Long shelterId);

    @Query("select st from ShelterStock st join fetch st.item where st.id.shelterId in :shelterIds")
    List<ShelterStock> findForShelters(@Param("shelterIds") Collection<Long> shelterIds);
}
