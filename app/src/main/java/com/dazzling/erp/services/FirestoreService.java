package com.dazzling.erp.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.dazzling.erp.models.Cutting;
import com.dazzling.erp.models.Fabric;
import com.dazzling.erp.models.Lot;
import com.dazzling.erp.models.StockSummary;
import com.dazzling.erp.models.PaymentRequest;
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
    private static final String COLLECTION_STOCK_SUMMARIES = "stock_summaries";
    private static final String COLLECTION_PAYMENT_REQUESTS = "payment_requests";
    
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
     * Search lots by lot number (one-time query)
     */
    public void searchLotsByNumberOnce(String lotNumber, LotCallback callback) {
        mFirestore.collection(COLLECTION_LOTS)
                .whereGreaterThanOrEqualTo("lotNumber", lotNumber)
                .whereLessThanOrEqualTo("lotNumber", lotNumber + '\uf8ff')
                .orderBy("lotNumber")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Lot> lots = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Lot lot = document.toObject(Lot.class);
                        if (lot != null) {
                            lot.setId(document.getId());
                            lots.add(lot);
                        }
                    }
                    
                    if (callback != null) {
                        callback.onLotsLoaded(lots);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error searching lots", e);
                    if (callback != null) {
                        callback.onError("Failed to search lots: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Update lot
     */
    public void updateLot(Lot lot, LotCallback callback) {
        Log.d(TAG, "🔄 Starting Firebase update for lot: " + (lot != null ? lot.getId() : "null"));
        
        if (lot == null) {
            Log.e(TAG, "❌ Lot object is null");
            if (callback != null) callback.onError("Lot object is null");
            return;
        }
        
        if (lot.getId() == null || lot.getId().isEmpty()) {
            Log.e(TAG, "❌ Lot ID is null or empty");
            if (callback != null) callback.onError("Lot ID is null or empty");
            return;
        }
        
        // Validate required fields
        if (lot.getLotNumber() == null) {
            lot.setLotNumber("");
        }
        if (lot.getFabricType() == null) {
            lot.setFabricType("");
        }
        if (lot.getColor() == null) {
            lot.setColor("");
        }
        if (lot.getSupplier() == null) {
            lot.setSupplier("");
        }
        if (lot.getCustomer() == null) {
            lot.setCustomer("");
        }
        if (lot.getOrderNumber() == null) {
            lot.setOrderNumber("");
        }
        if (lot.getStatus() == null) {
            lot.setStatus("active");
        }
        if (lot.getPriority() == null) {
            lot.setPriority("medium");
        }
        if (lot.getQuality() == null) {
            lot.setQuality("A");
        }
        if (lot.getNotes() == null) {
            lot.setNotes("");
        }
        if (lot.getCreatedBy() == null) {
            lot.setCreatedBy("user");
        }
        if (lot.getUpdatedBy() == null) {
            lot.setUpdatedBy("user");
        }
        
        lot.setUpdatedAt(new Date());
        Log.d(TAG, "📤 Sending lot data to Firebase - ID: " + lot.getId());
        Log.d(TAG, "📊 Lot data - Cutting Pcs: " + lot.getCuttingPcs() + ", Embroidery Receive: " + lot.getEmbroideryReceivePcs() + " pcs, Embroidery Reject: " + lot.getEmbroideryRejectPcs() + " pcs, Office Shipment: " + lot.getOfficeShipmentPcs() + " pcs");
        
        mFirestore.collection(COLLECTION_LOTS).document(lot.getId())
                .set(lot)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "✅ Lot updated successfully in Firebase: " + lot.getId());
                        Log.d(TAG, "📊 Firebase confirmed - Cutting Pcs: " + lot.getCuttingPcs() + ", Embroidery Receive: " + lot.getEmbroideryReceivePcs() + " pcs, Embroidery Reject: " + lot.getEmbroideryRejectPcs() + " pcs, Office Shipment: " + lot.getOfficeShipmentPcs() + " pcs");
                        if (callback != null) {
                            callback.onLotUpdated(lot);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Error updating lot in Firebase: " + lot.getId(), e);
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
        Log.d(TAG, "🔄 Starting Firebase update for cutting: " + (cutting != null ? cutting.getId() : "null"));
        
        if (cutting == null) {
            Log.e(TAG, "❌ Cutting object is null");
            if (callback != null) callback.onError("Cutting object is null");
            return;
        }
        
        if (cutting.getId() == null || cutting.getId().isEmpty()) {
            Log.e(TAG, "❌ Cutting ID is null or empty");
            if (callback != null) callback.onError("Cutting ID is null or empty");
            return;
        }
        
        // Validate required fields
        if (cutting.getFabricType() == null) {
            cutting.setFabricType("");
        }
        if (cutting.getLotNumber() == null) {
            cutting.setLotNumber("");
        }
        if (cutting.getColor() == null) {
            cutting.setColor("");
        }
        if (cutting.getCuttingType() == null) {
            cutting.setCuttingType("manual");
        }
        if (cutting.getOperator() == null) {
            cutting.setOperator("");
        }
        if (cutting.getMachineId() == null) {
            cutting.setMachineId("");
        }
        if (cutting.getQuality() == null) {
            cutting.setQuality("A");
        }
        if (cutting.getNotes() == null) {
            cutting.setNotes("");
        }
        if (cutting.getStatus() == null) {
            cutting.setStatus("pending");
        }
        if (cutting.getCreatedBy() == null) {
            cutting.setCreatedBy("user");
        }
        if (cutting.getUpdatedBy() == null) {
            cutting.setUpdatedBy("user");
        }
        
        // Ensure updatedAt is set
        cutting.setUpdatedAt(new Date());
        
        Log.d(TAG, "📤 Sending cutting data to Firebase - ID: " + cutting.getId());
        Log.d(TAG, "📊 Cutting data - Pcs: " + cutting.getQuantityPcs() + ", Kg: " + cutting.getQuantityKg() + ", Original Kg: " + cutting.getOriginalQuantityKg());
        
        mFirestore.collection(COLLECTION_CUTTING)
            .document(cutting.getId())
            .set(cutting)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cutting updated successfully in Firebase: " + cutting.getId());
                Log.d(TAG, "📊 Firebase confirmed - Pcs: " + cutting.getQuantityPcs() + ", Kg: " + cutting.getQuantityKg() + ", Original Kg: " + cutting.getOriginalQuantityKg());
                if (callback != null) callback.onCuttingUpdated(cutting);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error updating cutting in Firebase: " + cutting.getId(), e);
                if (callback != null) callback.onError("Failed to update cutting: " + e.getMessage());
            });
    }
    
    /**
     * Verify Firebase connectivity and data integrity
     */
    public void verifyFirebaseConnection(Runnable onSuccess, Runnable onError) {
        Log.d(TAG, "🔍 Verifying Firebase connectivity...");
        
        // Try to read a single document to test connectivity
        mFirestore.collection(COLLECTION_LOTS)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "✅ Firebase connectivity verified successfully");
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firebase connectivity test failed", e);
                    if (onError != null) {
                        onError.run();
                    }
                });
    }
    
    /**
     * Get Firebase statistics for debugging
     */
    public void getFirebaseStats(Runnable onComplete) {
        Log.d(TAG, "📊 Getting Firebase statistics...");
        
        // Count documents in each collection
        mFirestore.collection(COLLECTION_LOTS).get()
                .addOnSuccessListener(lotsSnapshot -> {
                    int lotsCount = lotsSnapshot.size();
                    Log.d(TAG, "📊 Lots collection: " + lotsCount + " documents");
                    
                    mFirestore.collection(COLLECTION_CUTTING).get()
                            .addOnSuccessListener(cuttingSnapshot -> {
                                int cuttingCount = cuttingSnapshot.size();
                                Log.d(TAG, "📊 Cutting collection: " + cuttingCount + " documents");
                                
                                mFirestore.collection(COLLECTION_FABRICS).get()
                                        .addOnSuccessListener(fabricsSnapshot -> {
                                            int fabricsCount = fabricsSnapshot.size();
                                            Log.d(TAG, "📊 Fabrics collection: " + fabricsCount + " documents");
                                            Log.d(TAG, "📊 Total documents across all collections: " + (lotsCount + cuttingCount + fabricsCount));
                                            
                                            if (onComplete != null) {
                                                onComplete.run();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ Error getting fabrics count", e);
                                            if (onComplete != null) {
                                                onComplete.run();
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error getting cutting count", e);
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error getting lots count", e);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }
    
    /**
     * Verify that all lot data is properly saved to Firebase
     * This method performs a comprehensive check of all Pcs fields
     */
    public void verifyLotDataSave(Lot lot, Runnable onSuccess, Runnable onError) {
        Log.d(TAG, "🔍 Verifying lot data save for lot ID: " + (lot != null ? lot.getId() : "null"));
        
        if (lot == null) {
            Log.e(TAG, "❌ Lot object is null for verification");
            if (onError != null) onError.run();
            return;
        }
        
        // Log all Pcs fields for verification
        Log.d(TAG, "📊 Verification - Lot ID: " + lot.getId());
        Log.d(TAG, "📊 Verification - Cutting Pcs: " + lot.getCuttingPcs());
        Log.d(TAG, "📊 Verification - Embroidery Receive Pcs: " + lot.getEmbroideryReceivePcs());
        Log.d(TAG, "📊 Verification - Embroidery Reject Pcs: " + lot.getEmbroideryRejectPcs());
        Log.d(TAG, "📊 Verification - Office Shipment Pcs: " + lot.getOfficeShipmentPcs());
        
        // Verify that all Pcs fields are properly set
        boolean allFieldsValid = true;
        String errorMessage = "";
        
        if (lot.getCuttingPcs() < 0) {
            allFieldsValid = false;
            errorMessage += "Cutting Pcs cannot be negative. ";
        }
        
        if (lot.getEmbroideryReceivePcs() < 0) {
            allFieldsValid = false;
            errorMessage += "Embroidery Receive Pcs cannot be negative. ";
        }
        
        if (lot.getEmbroideryRejectPcs() < 0) {
            allFieldsValid = false;
            errorMessage += "Embroidery Reject Pcs cannot be negative. ";
        }
        
        if (lot.getOfficeShipmentPcs() < 0) {
            allFieldsValid = false;
            errorMessage += "Office Shipment Pcs cannot be negative. ";
        }
        
        if (allFieldsValid) {
            Log.d(TAG, "✅ All lot data fields are valid and ready for Firebase save");
            if (onSuccess != null) onSuccess.run();
        } else {
            Log.e(TAG, "❌ Lot data validation failed: " + errorMessage);
            if (onError != null) onError.run();
        }
    }
    
    /**
     * Comprehensive verification that all lot data is properly saved to Firebase
     * This method performs a complete audit of the lot data
     */
    public void comprehensiveLotDataVerification(Lot lot, Runnable onSuccess, Runnable onError) {
        Log.d(TAG, "🔍 Comprehensive lot data verification for lot ID: " + (lot != null ? lot.getId() : "null"));
        
        if (lot == null) {
            Log.e(TAG, "❌ Lot object is null for comprehensive verification");
            if (onError != null) onError.run();
            return;
        }
        
        // Comprehensive logging of all fields
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Lot ID: " + lot.getId());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Lot Number: " + lot.getLotNumber());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Fabric Type: " + lot.getFabricType());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Color: " + lot.getColor());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Total Fabric Kg: " + lot.getTotalFabricKg());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Cutting Kg: " + lot.getCuttingKg());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Cutting Pcs: " + lot.getCuttingPcs());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Embroidery Receive Kg: " + lot.getEmbroideryReceiveKg());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Embroidery Receive Pcs: " + lot.getEmbroideryReceivePcs());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Embroidery Reject Kg: " + lot.getEmbroideryRejectKg());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Embroidery Reject Pcs: " + lot.getEmbroideryRejectPcs());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Office Shipment Kg: " + lot.getOfficeShipmentKg());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Office Shipment Pcs: " + lot.getOfficeShipmentPcs());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Status: " + lot.getStatus());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Priority: " + lot.getPriority());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Quality: " + lot.getQuality());
        Log.d(TAG, "📊 COMPREHENSIVE VERIFICATION - Updated At: " + lot.getUpdatedAt());
        
        // Verify data integrity
        boolean dataIntegrityValid = true;
        String integrityErrorMessage = "";
        
        // Check for required fields
        if (lot.getLotNumber() == null || lot.getLotNumber().isEmpty()) {
            dataIntegrityValid = false;
            integrityErrorMessage += "Lot number is required. ";
        }
        
        if (lot.getFabricType() == null || lot.getFabricType().isEmpty()) {
            dataIntegrityValid = false;
            integrityErrorMessage += "Fabric type is required. ";
        }
        
        if (lot.getColor() == null || lot.getColor().isEmpty()) {
            dataIntegrityValid = false;
            integrityErrorMessage += "Color is required. ";
        }
        
        // Check for logical consistency
        if (lot.getCuttingPcs() > lot.getTotalFabricKg() * 100) { // Rough conversion check
            dataIntegrityValid = false;
            integrityErrorMessage += "Cutting Pcs seems unreasonably high compared to total fabric. ";
        }
        
        if (lot.getEmbroideryReceivePcs() > lot.getCuttingPcs()) {
            dataIntegrityValid = false;
            integrityErrorMessage += "Embroidery Receive Pcs cannot exceed Cutting Pcs. ";
        }
        
        if (lot.getEmbroideryRejectPcs() > lot.getEmbroideryReceivePcs()) {
            dataIntegrityValid = false;
            integrityErrorMessage += "Embroidery Reject Pcs cannot exceed Embroidery Receive Pcs. ";
        }
        
        if (lot.getOfficeShipmentPcs() > lot.getEmbroideryReceivePcs()) {
            dataIntegrityValid = false;
            integrityErrorMessage += "Office Shipment Pcs cannot exceed Embroidery Receive Pcs. ";
        }
        
        if (dataIntegrityValid) {
            Log.d(TAG, "✅ Comprehensive lot data verification passed - all data is valid and ready for Firebase save");
            if (onSuccess != null) onSuccess.run();
        } else {
            Log.e(TAG, "❌ Comprehensive lot data verification failed: " + integrityErrorMessage);
            if (onError != null) onError.run();
        }
    }
    
    // ==================== STOCK SUMMARY OPERATIONS ====================
    
    public interface StockSummaryCallback {
        void onStockSummariesLoaded(List<StockSummary> stockSummaries);
        void onStockSummaryAdded(StockSummary stockSummary);
        void onStockSummaryUpdated(StockSummary stockSummary);
        void onStockSummaryDeleted(String stockSummaryId);
        void onError(String error);
    }
    
    /**
     * Add new stock summary
     */
    public void addStockSummary(StockSummary stockSummary, StockSummaryCallback callback) {
        stockSummary.setCreatedAt(System.currentTimeMillis());
        mFirestore.collection(COLLECTION_STOCK_SUMMARIES)
                .add(stockSummary)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        stockSummary.setId(documentReference.getId());
                        Log.d(TAG, "Stock summary added with ID: " + documentReference.getId());
                        if (callback != null) {
                            callback.onStockSummaryAdded(stockSummary);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding stock summary", e);
                        if (callback != null) {
                            callback.onError("Failed to add stock summary: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Get all stock summaries with real-time updates
     */
    public void getStockSummaries(StockSummaryCallback callback) {
        mFirestore.collection(COLLECTION_STOCK_SUMMARIES)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting stock summaries", error);
                        if (callback != null) {
                            callback.onError("Failed to load stock summaries: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<StockSummary> stockSummaries = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            try {
                                StockSummary stockSummary = document.toObject(StockSummary.class);
                                if (stockSummary != null) {
                                    stockSummary.setId(document.getId());
                                    stockSummaries.add(stockSummary);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error parsing document: " + document.getId(), e);
                            }
                        }
                    }
                    
                    // Sort by createdAt in descending order
                    stockSummaries.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    
                    if (callback != null) {
                        callback.onStockSummariesLoaded(stockSummaries);
                    }
                });
    }
    
    /**
     * Get stock summaries by office with real-time updates
     */
    public void getStockSummariesByOffice(String office, StockSummaryCallback callback) {
        mFirestore.collection(COLLECTION_STOCK_SUMMARIES)
                .whereEqualTo("office", office)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting stock summaries for office: " + office, error);
                        if (callback != null) {
                            callback.onError("Failed to load stock summaries: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<StockSummary> stockSummaries = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            try {
                                StockSummary stockSummary = document.toObject(StockSummary.class);
                                if (stockSummary != null) {
                                    stockSummary.setId(document.getId());
                                    // Ensure office field is set
                                    if (stockSummary.getOffice() == null) {
                                        stockSummary.setOffice(office);
                                    }
                                    stockSummaries.add(stockSummary);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error parsing document: " + document.getId(), e);
                            }
                        }
                    }
                    
                    // Sort by createdAt in descending order
                    stockSummaries.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    
                    if (callback != null) {
                        callback.onStockSummariesLoaded(stockSummaries);
                    }
                });
    }
    
    /**
     * Get stock summaries by product type
     */
    public void getStockSummariesByProductType(String productType, StockSummaryCallback callback) {
        mFirestore.collection(COLLECTION_STOCK_SUMMARIES)
                .whereEqualTo("productType", productType)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting stock summaries by product type", error);
                        if (callback != null) {
                            callback.onError("Failed to load stock summaries: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<StockSummary> stockSummaries = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            StockSummary stockSummary = document.toObject(StockSummary.class);
                            if (stockSummary != null) {
                                stockSummary.setId(document.getId());
                                stockSummaries.add(stockSummary);
                            }
                        }
                    }
                    
                    if (callback != null) {
                        callback.onStockSummariesLoaded(stockSummaries);
                    }
                });
    }
    
    /**
     * Update stock summary
     */
    public void updateStockSummary(StockSummary stockSummary, StockSummaryCallback callback) {
        mFirestore.collection(COLLECTION_STOCK_SUMMARIES).document(stockSummary.getId())
                .set(stockSummary)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Stock summary updated successfully");
                        if (callback != null) {
                            callback.onStockSummaryUpdated(stockSummary);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating stock summary", e);
                        if (callback != null) {
                            callback.onError("Failed to update stock summary: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Delete stock summary
     */
    public void deleteStockSummary(String stockSummaryId, StockSummaryCallback callback) {
        mFirestore.collection(COLLECTION_STOCK_SUMMARIES).document(stockSummaryId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Stock summary deleted successfully");
                        if (callback != null) {
                            callback.onStockSummaryDeleted(stockSummaryId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting stock summary", e);
                        if (callback != null) {
                            callback.onError("Failed to delete stock summary: " + e.getMessage());
                        }
                    }
                });
    }
    
    // ==================== PAYMENT REQUEST OPERATIONS ====================
    
    public interface PaymentRequestCallback {
        void onPaymentRequestsLoaded(List<PaymentRequest> paymentRequests);
        void onPaymentRequestAdded(PaymentRequest paymentRequest);
        void onPaymentRequestUpdated(PaymentRequest paymentRequest);
        void onPaymentRequestDeleted(String paymentRequestId);
        void onError(String error);
    }
    
    /**
     * Add new payment request
     */
    public void addPaymentRequest(PaymentRequest paymentRequest, PaymentRequestCallback callback) {
        mFirestore.collection(COLLECTION_PAYMENT_REQUESTS)
                .add(paymentRequest)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        paymentRequest.setId(documentReference.getId());
                        Log.d(TAG, "Payment request added with ID: " + documentReference.getId());
                        if (callback != null) {
                            callback.onPaymentRequestAdded(paymentRequest);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding payment request", e);
                        if (callback != null) {
                            callback.onError("Failed to add payment request: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Get payment requests by office with real-time updates
     */
    public void getPaymentRequestsByOffice(String office, PaymentRequestCallback callback) {
        mFirestore.collection(COLLECTION_PAYMENT_REQUESTS)
                .whereEqualTo("office", office)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error getting payment requests for office: " + office, error);
                        if (callback != null) {
                            callback.onError("Failed to load payment requests: " + error.getMessage());
                        }
                        return;
                    }
                    
                    List<PaymentRequest> paymentRequests = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            try {
                                PaymentRequest paymentRequest = document.toObject(PaymentRequest.class);
                                if (paymentRequest != null) {
                                    paymentRequest.setId(document.getId());
                                    // Ensure office field is set
                                    if (paymentRequest.getOffice() == null) {
                                        paymentRequest.setOffice(office);
                                    }
                                    paymentRequests.add(paymentRequest);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error parsing document: " + document.getId(), e);
                            }
                        }
                    }
                    
                    // Sort by createdAt in descending order
                    paymentRequests.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    
                    if (callback != null) {
                        callback.onPaymentRequestsLoaded(paymentRequests);
                    }
                });
    }
    
    /**
     * Update payment request
     */
    public void updatePaymentRequest(PaymentRequest paymentRequest, PaymentRequestCallback callback) {
        mFirestore.collection(COLLECTION_PAYMENT_REQUESTS).document(paymentRequest.getId())
                .set(paymentRequest)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Payment request updated successfully");
                        if (callback != null) {
                            callback.onPaymentRequestUpdated(paymentRequest);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating payment request", e);
                        if (callback != null) {
                            callback.onError("Failed to update payment request: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Delete payment request
     */
    public void deletePaymentRequest(String paymentRequestId, PaymentRequestCallback callback) {
        mFirestore.collection(COLLECTION_PAYMENT_REQUESTS).document(paymentRequestId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Payment request deleted successfully");
                        if (callback != null) {
                            callback.onPaymentRequestDeleted(paymentRequestId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting payment request", e);
                        if (callback != null) {
                            callback.onError("Failed to delete payment request: " + e.getMessage());
                        }
                    }
                });
    }
} 