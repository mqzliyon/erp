package com.dazzling.erp.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.dazzling.erp.models.Cutting;
import com.dazzling.erp.models.Fabric;
import com.dazzling.erp.models.Lot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Firestore Service for handling all data operations
 */
public class FirestoreService {
    private static final String TAG = "FirestoreService";
    
    private FirebaseFirestore mFirestore;
    
    // Collection names
    private static final String COLLECTION_FABRICS = "fabrics";
    private static final String COLLECTION_CUTTING = "cutting";
    private static final String COLLECTION_LOTS = "lots";
    private static final String COLLECTION_USERS = "users";
    
    public FirestoreService() {
        mFirestore = FirebaseFirestore.getInstance();
    }
    
    // ==================== FABRIC OPERATIONS ====================
    
    public interface FabricCallback {
        void onFabricsLoaded(List<Fabric> fabrics);
        void onFabricAdded(Fabric fabric);
        void onFabricUpdated(Fabric fabric);
        void onFabricDeleted(String fabricId);
        void onError(String error);
    }
    
    /**
     * Add new fabric
     */
    public void addFabric(Fabric fabric, FabricCallback callback) {
        fabric.setCreatedAt(new Date());
        fabric.setUpdatedAt(new Date());
        mFirestore.collection(COLLECTION_FABRICS)
                .add(fabric)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        fabric.setId(documentReference.getId());
                        Log.d(TAG, "Fabric added with ID: " + documentReference.getId());
                        if (callback != null) {
                            callback.onFabricAdded(fabric);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding fabric", e);
                        if (callback != null) {
                            callback.onError("Failed to add fabric: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Get all fabrics with real-time updates
     */
    public void getFabrics(FabricCallback callback) {
        mFirestore.collection(COLLECTION_FABRICS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting fabrics", error);
                        if (callback != null) {
                            callback.onError("Failed to load fabrics: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<Fabric> fabrics = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Fabric fabric = document.toObject(Fabric.class);
                            if (fabric != null) {
                                fabric.setId(document.getId());
                                fabrics.add(fabric);
                            }
                        }
                    }
                    
                    if (callback != null) {
                        callback.onFabricsLoaded(fabrics);
                    }
                });
    }
    
    /**
     * Update fabric
     */
    public void updateFabric(Fabric fabric, FabricCallback callback) {
        fabric.setUpdatedAt(new Date());
        mFirestore.collection(COLLECTION_FABRICS).document(fabric.getId())
                .set(fabric)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Fabric updated successfully");
                        if (callback != null) {
                            callback.onFabricUpdated(fabric);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating fabric", e);
                        if (callback != null) {
                            callback.onError("Failed to update fabric: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Delete fabric
     */
    public void deleteFabric(String fabricId, FabricCallback callback) {
        mFirestore.collection(COLLECTION_FABRICS).document(fabricId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Fabric deleted successfully");
                        if (callback != null) {
                            callback.onFabricDeleted(fabricId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting fabric", e);
                        if (callback != null) {
                            callback.onError("Failed to delete fabric: " + e.getMessage());
                        }
                    }
                });
    }
    
    // ==================== CUTTING OPERATIONS ====================
    
    public interface CuttingCallback {
        void onCuttingsLoaded(List<Cutting> cuttings);
        void onCuttingAdded(Cutting cutting);
        void onCuttingUpdated(Cutting cutting);
        void onCuttingDeleted(String cuttingId);
        void onError(String error);
    }
    
    /**
     * Add new cutting operation
     */
    public void addCutting(Cutting cutting, CuttingCallback callback) {
        mFirestore.collection(COLLECTION_CUTTING)
                .add(cutting)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        cutting.setId(documentReference.getId());
                        Log.d(TAG, "Cutting added with ID: " + documentReference.getId());
                        if (callback != null) {
                            callback.onCuttingAdded(cutting);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding cutting", e);
                        if (callback != null) {
                            callback.onError("Failed to add cutting: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Get all cutting operations with real-time updates
     */
    public void getCuttings(CuttingCallback callback) {
        mFirestore.collection(COLLECTION_CUTTING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting cuttings", error);
                        if (callback != null) {
                            callback.onError("Failed to load cuttings: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<Cutting> cuttings = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Cutting cutting = document.toObject(Cutting.class);
                            if (cutting != null) {
                                cutting.setId(document.getId());
                                cuttings.add(cutting);
                            }
                        }
                    }
                    
                    if (callback != null) {
                        callback.onCuttingsLoaded(cuttings);
                    }
                });
    }
    
    /**
     * Get cuttings by lot number
     */
    public void getCuttingsByLot(String lotNumber, CuttingCallback callback) {
        mFirestore.collection(COLLECTION_CUTTING)
                .whereEqualTo("lotNumber", lotNumber)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting cuttings by lot", error);
                        if (callback != null) {
                            callback.onError("Failed to load cuttings: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<Cutting> cuttings = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Cutting cutting = document.toObject(Cutting.class);
                            if (cutting != null) {
                                cutting.setId(document.getId());
                                cuttings.add(cutting);
                            }
                        }
                    }
                    
                    if (callback != null) {
                        callback.onCuttingsLoaded(cuttings);
                    }
                });
    }
    
    // Cutting: Delete cutting entry by ID
    public void deleteCutting(String cuttingId, CuttingCallback callback) {
        mFirestore.collection(COLLECTION_CUTTING).document(cuttingId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Cutting deleted successfully");
                if (callback != null) {
                    callback.onCuttingDeleted(cuttingId);
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error deleting cutting", e);
                if (callback != null) {
                    callback.onError("Failed to delete cutting: " + e.getMessage());
                }
            });
    }
    
    // ==================== LOT OPERATIONS ====================
    
    public interface LotCallback {
        void onLotsLoaded(List<Lot> lots);
        void onLotAdded(Lot lot);
        void onLotUpdated(Lot lot);
        void onLotDeleted(String lotId);
        void onError(String error);
    }
    
    /**
     * Add new lot
     */
    public void addLot(Lot lot, LotCallback callback) {
        mFirestore.collection(COLLECTION_LOTS)
                .add(lot)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        lot.setId(documentReference.getId());
                        Log.d(TAG, "Lot added with ID: " + documentReference.getId());
                        if (callback != null) {
                            callback.onLotAdded(lot);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding lot", e);
                        if (callback != null) {
                            callback.onError("Failed to add lot: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Get all lots with real-time updates
     */
    public void getLots(LotCallback callback) {
        mFirestore.collection(COLLECTION_LOTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting lots", error);
                        if (callback != null) {
                            callback.onError("Failed to load lots: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<Lot> lots = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Lot lot = document.toObject(Lot.class);
                            if (lot != null) {
                                lot.setId(document.getId());
                                lots.add(lot);
                            }
                        }
                    }
                    
                    if (callback != null) {
                        callback.onLotsLoaded(lots);
                    }
                });
    }
    
    /**
     * Get lot by ID
     */
    public void getLotById(String lotId, LotCallback callback) {
        mFirestore.collection(COLLECTION_LOTS).document(lotId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Lot lot = documentSnapshot.toObject(Lot.class);
                            if (lot != null) {
                                lot.setId(documentSnapshot.getId());
                                List<Lot> lots = new ArrayList<>();
                                lots.add(lot);
                                if (callback != null) {
                                    callback.onLotsLoaded(lots);
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("Lot not found");
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error getting lot", e);
                        if (callback != null) {
                            callback.onError("Failed to load lot: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Search lots by lot number
     */
    public void searchLotsByNumber(String lotNumber, LotCallback callback) {
        mFirestore.collection(COLLECTION_LOTS)
                .whereGreaterThanOrEqualTo("lotNumber", lotNumber)
                .whereLessThanOrEqualTo("lotNumber", lotNumber + '\uf8ff')
                .orderBy("lotNumber")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error searching lots", error);
                        if (callback != null) {
                            callback.onError("Failed to search lots: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<Lot> lots = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Lot lot = document.toObject(Lot.class);
                            if (lot != null) {
                                lot.setId(document.getId());
                                lots.add(lot);
                            }
                        }
                    }
                    
                    if (callback != null) {
                        callback.onLotsLoaded(lots);
                    }
                });
    }
    
    /**
     * Update lot
     */
    public void updateLot(Lot lot, LotCallback callback) {
        lot.setUpdatedAt(new Date());
        mFirestore.collection(COLLECTION_LOTS).document(lot.getId())
                .set(lot)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Lot updated successfully");
                        if (callback != null) {
                            callback.onLotUpdated(lot);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating lot", e);
                        if (callback != null) {
                            callback.onError("Failed to update lot: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Delete lot
     */
    public void deleteLot(String lotId, LotCallback callback) {
        mFirestore.collection(COLLECTION_LOTS).document(lotId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Lot deleted successfully");
                        if (callback != null) {
                            callback.onLotDeleted(lotId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting lot", e);
                        if (callback != null) {
                            callback.onError("Failed to delete lot: " + e.getMessage());
                        }
                    }
                });
    }

    // Fabric: Return quantity to fabric by type, color, and lotNumber
    public void returnQuantityToFabric(String fabricType, String color, String lotNumber, double qty, FabricCallback callback) {
        Log.d(TAG, "Attempting to return quantity to fabric: type=" + fabricType + ", color=" + color + ", lotNumber=" + lotNumber + ", qty=" + qty);
        mFirestore.collection(COLLECTION_FABRICS)
            .whereEqualTo("fabricType", fabricType)
            .whereEqualTo("color", color)
            .whereEqualTo("lotNumber", lotNumber)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    Fabric fabric = document.toObject(Fabric.class);
                    if (fabric != null) {
                        fabric.setId(document.getId());
                        double newQty = fabric.getQuantityKg() + qty;
                        Log.d(TAG, "Found fabric (id=" + fabric.getId() + ") oldQty=" + fabric.getQuantityKg() + ", newQty=" + newQty);
                        fabric.setQuantityKg(newQty);
                        fabric.setUpdatedAt(new java.util.Date());
                        // Add a return entry to transferHistory
                        List<com.dazzling.erp.models.Transfer> history = fabric.getTransferHistory();
                        if (history == null) history = new java.util.ArrayList<>();
                        history.add(new com.dazzling.erp.models.Transfer(qty, new java.util.Date(), "Return"));
                        fabric.setTransferHistory(history);
                        mFirestore.collection(COLLECTION_FABRICS).document(fabric.getId())
                            .set(fabric)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Fabric quantity returned successfully");
                                if (callback != null) {
                                    callback.onFabricUpdated(fabric);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error updating fabric quantity", e);
                                if (callback != null) {
                                    callback.onError("Failed to update fabric: " + e.getMessage());
                                }
                            });
                    } else {
                        Log.e(TAG, "Fabric found but could not be converted to object");
                        if (callback != null) {
                            callback.onError("Fabric not found");
                        }
                    }
                } else {
                    Log.e(TAG, "No fabric found for type=" + fabricType + ", color=" + color + ", lotNumber=" + lotNumber);
                    if (callback != null) {
                        callback.onError("Fabric not found");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error finding fabric", e);
                if (callback != null) {
                    callback.onError("Failed to find fabric: " + e.getMessage());
                }
            });
    }

    /**
     * Update cutting operation (ALWAYS call this after any in-memory change to pcs/kg/originalQuantityKg)
     */
    public void updateCutting(Cutting cutting, CuttingCallback callback) {
        if (cutting.getId() == null) {
            if (callback != null) callback.onError("Cutting ID is null");
            return;
        }
        mFirestore.collection(COLLECTION_CUTTING)
            .document(cutting.getId())
            .set(cutting)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onCuttingUpdated(cutting);
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }
} 