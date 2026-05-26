import React, { useState, useEffect, useRef } from 'react';
import './App.css';

function App() {
  // Authentication State
  const [token, setToken] = useState(localStorage.getItem('token') || null);
  const [userSession, setUserSession] = useState(null);
  const [authError, setAuthError] = useState('');
  const [authSuccess, setAuthSuccess] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);
  const [authUsername, setAuthUsername] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authRole, setAuthRole] = useState('ROLE_ADMIN');

  // Parking Dashboard State
  const [slots, setSlots] = useState([]);
  const [revenue, setRevenue] = useState(380);
  const [occupiedCount, setOccupiedCount] = useState(0);
  const [vacantCount, setVacantCount] = useState(20);
  const [isConnected, setIsConnected] = useState(false);

  // Billing Microservice State
  const [invoices, setInvoices] = useState([]);
  const [showInvoices, setShowInvoices] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [showTerminal, setShowTerminal] = useState(false);

  // Forms inputs
  const [parkPlate, setParkPlate] = useState('');
  const [parkType, setParkType] = useState('CAR');
  const [exitPlate, setExitPlate] = useState('');
  const [exitHours, setExitHours] = useState(1);

  // Logs
  const [logs, setLogs] = useState([]);
  
  // Modals
  const [receipt, setReceipt] = useState(null);

  // Refs for scrolling logs
  const logsEndRef = useRef(null);

  // API Gateways
  const API_URL = window.location.origin.includes('localhost') 
    ? 'http://localhost:8080/api'
    : `${window.location.origin}/api`;

  const AUTH_URL = window.location.origin.includes('localhost')
    ? 'http://localhost:8081/api/auth'
    : `${window.location.origin.replace('8080', '8081')}/api/auth`;

  const BILLING_URL = window.location.origin.includes('localhost')
    ? 'http://localhost:8082/api/billing'
    : `${window.location.origin.replace('8080', '8082')}/api/billing`;

  // Sandbox fallback helpers
  const platePrefixes = ['MH-12', 'DL-3C', 'KA-03', 'KA-51', 'HR-26', 'UP-16', 'MH-02'];
  const mockBrands = {
    'Car': ['Tesla Model Y', 'Honda Civic', 'Toyota Camry', 'BMW 3 Series', 'Audi A4', 'Hyundai i20'],
    'Bike': ['Yamaha R15', 'Royal Enfield', 'KTM Duke', 'Honda Activa', 'Harley Davidson']
  };

  const generateRandomPlate = () => {
    const prefix = platePrefixes[Math.floor(Math.random() * platePrefixes.length)];
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const letters = alphabet[Math.floor(Math.random() * 26)] + alphabet[Math.floor(Math.random() * 26)];
    const numbers = Math.floor(1000 + Math.random() * 9000);
    return `${prefix}-${letters}-${numbers}`;
  };

  const addLog = (message, type = 'info') => {
    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    setLogs(prev => [...prev, { time: timeStr, text: message, type }]);
  };

  // Check stored token validity on launch
  useEffect(() => {
    const checkToken = async () => {
      if (!token) return;
      try {
        const res = await fetch(`${AUTH_URL}/validate?token=${token}`);
        if (res.ok) {
          const data = await res.json();
          if (data.valid) {
            setUserSession({ username: data.username, role: data.role });
            addLog(`Authenticated successfully as '${data.username}' [JWT Session Verified].`, 'success');
          } else {
            handleLogout();
            addLog("Stored session expired or invalid. Please log in.", 'error');
          }
        }
      } catch (e) {
        // Fallback for demo when auth-service might be offline
        setUserSession({ username: 'Demo Admin', role: 'ROLE_ADMIN' });
        addLog("Auth microservice offline. Authenticated in Demo Sandbox mode.", 'info');
      }
    };
    checkToken();
  }, [token]);

  // Seed local slots for sandbox fallback
  const seedSandbox = () => {
    const initialSlots = [];
    const capacity = 20;
    let occCount = 0;
    
    for (let i = 1; i <= capacity; i++) {
      const isOccupied = Math.random() > 0.65;
      if (isOccupied) {
        occCount++;
        const type = Math.random() > 0.5 ? 'Car' : 'Bike';
        const brands = mockBrands[type];
        initialSlots.push({
          slotNumber: i,
          occupied: true,
          vehicleNumber: generateRandomPlate(),
          vehicleType: type,
          brand: brands[Math.floor(Math.random() * brands.length)],
          entryTime: new Date().toISOString()
        });
      } else {
        initialSlots.push({
          slotNumber: i,
          occupied: false,
          vehicleNumber: null,
          vehicleType: null,
          brand: null,
          entryTime: null
        });
      }
    }
    setSlots(initialSlots);
    setOccupiedCount(occCount);
    setVacantCount(capacity - occCount);
    setRevenue(380);
  };

  // Check backend server status
  const checkConnection = async () => {
    try {
      const res = await fetch(`${API_URL}/status`);
      if (res.ok) {
        const data = await res.json();
        if (data.connected) {
          setIsConnected(true);
          return true;
        }
      }
    } catch (e) {
      // Offline
    }
    setIsConnected(false);
    return false;
  };

  // Fetch slots & revenue from Spring Boot
  const fetchData = async () => {
    try {
      const slotsRes = await fetch(`${API_URL}/slots`);
      if (!slotsRes.ok) throw new Error('Slots fail');
      const dbSlots = await slotsRes.json();
      
      const mapped = dbSlots.map(dbSlot => ({
        slotNumber: dbSlot.slotNumber,
        occupied: dbSlot.vehicleNumber !== null,
        vehicleNumber: dbSlot.vehicleNumber,
        vehicleType: dbSlot.vehicleType === 'CAR' ? 'Car' : 'Bike',
        brand: dbSlot.vehicleType === 'CAR' ? 'Tesla Model S' : 'Royal Enfield',
        entryTime: dbSlot.entryTime
      }));

      setSlots(mapped);
      
      const occupied = mapped.filter(s => s.occupied).length;
      setOccupiedCount(occupied);
      setVacantCount(mapped.length - occupied);

      const revRes = await fetch(`${API_URL}/revenue`);
      if (revRes.ok) {
        const revData = await revRes.json();
        setRevenue(revData.revenue);
      }
    } catch (err) {
      setIsConnected(false);
    }
  };

  // Fetch billing history from Billing Service
  const fetchInvoices = async () => {
    try {
      const res = await fetch(`${BILLING_URL}/invoices`);
      if (res.ok) {
        const data = await res.json();
        setInvoices(data);
      }
    } catch (err) {
      // Silent error when billing-service is offline
    }
  };

  // Mount logic for logged-in user
  useEffect(() => {
    if (!userSession) return;

    const init = async () => {
      const connected = await checkConnection();
      if (connected) {
        await fetchData();
        addLog("Connected to remote Railway MySQL Database cluster via Spring Boot REST API.", "success");
      } else {
        seedSandbox();
        addLog("Local database offline. Initialized local Sandbox simulation.", "info");
      }
      // Load billing data
      await fetchInvoices();
    };
    init();

    // Background sync interval (every 3 seconds)
    const interval = setInterval(async () => {
      const connected = await checkConnection();
      if (connected) {
        await fetchData();
      }
      await fetchInvoices();
    }, 3000);

    return () => clearInterval(interval);
  }, [isConnected, userSession]);

  // Scroll logs to bottom
  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  // Handle Authentication Logic
  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    setAuthError('');
    setAuthSuccess('');

    if (!authUsername.trim() || !authPassword.trim()) {
      setAuthError('Please fill in all credentials fields.');
      return;
    }

    try {
      if (isRegistering) {
        // Register API Call
        const res = await fetch(`${AUTH_URL}/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: authUsername, password: authPassword, role: authRole })
        });
        const data = await res.json();
        if (res.ok) {
          setAuthSuccess('Account registered successfully! You can now log in.');
          setIsRegistering(false);
          setAuthPassword('');
        } else {
          setAuthError(data.error || 'Registration failed.');
        }
      } else {
        // Login API Call
        const res = await fetch(`${AUTH_URL}/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: authUsername, password: authPassword })
        });
        const data = await res.json();
        if (res.ok) {
          localStorage.setItem('token', data.token);
          setToken(data.token);
        } else {
          setAuthError(data.error || 'Invalid credentials.');
        }
      }
    } catch (err) {
      addLog("Auth microservice connection error. Failed to reach server.", "error");
      setAuthError("Unable to connect to the authentication server. Please ensure the auth service is running.");
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUserSession(null);
    setAuthUsername('');
    setAuthPassword('');
    addLog("User logged out successfully. Session revoked.", "info");
  };

  const prefillDemoCredentials = () => {
    setAuthUsername('admin');
    setAuthPassword('password123');
  };

  // Park operation
  const handlePark = async (e) => {
    if (e) e.preventDefault();
    if (!parkPlate.trim()) return;
    const plate = parkPlate.trim().toUpperCase();

    if (isConnected) {
      try {
        const res = await fetch(`${API_URL}/slots/park`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ plate, type: parkType })
        });
        const data = await res.json();
        if (data.success) {
          addLog(`Vehicle [${plate}] parked successfully at Slot #${data.slotNumber}.`, 'success');
          setParkPlate('');
          await fetchData();
        } else {
          addLog(`Parking failed: ${data.message}`, 'error');
        }
      } catch (err) {
        addLog("Communication failure with REST backend.", 'error');
      }
    } else {
      // Sandbox fallback
      const freeIdx = slots.findIndex(s => !s.occupied);
      if (freeIdx === -1) {
        addLog("Parking failed: slots are full.", 'error');
        return;
      }
      
      const newSlots = [...slots];
      newSlots[freeIdx] = {
        slotNumber: freeIdx + 1,
        occupied: true,
        vehicleNumber: plate,
        vehicleType: parkType === 'CAR' ? 'Car' : 'Bike',
        brand: parkType === 'CAR' ? 'BMW 3 Series' : 'KTM Duke',
        entryTime: new Date().toISOString()
      };
      
      setSlots(newSlots);
      setOccupiedCount(prev => prev + 1);
      setVacantCount(prev => prev - 1);
      addLog(`[Sandbox] Parked Vehicle [${plate}] at Slot #${freeIdx + 1}.`, 'success');
      setParkPlate('');
    }
  };

  // Exit operation - integrated E2E with Billing microservice
  const handleExit = async (e) => {
    if (e) e.preventDefault();
    if (!exitPlate.trim()) return;
    const plate = exitPlate.trim().toUpperCase();

    if (isConnected) {
      try {
        // 1. Release slot in core app mysql db
        const res = await fetch(`${API_URL}/slots/release`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ plate, hours: exitHours })
        });
        const data = await res.json();
        
        if (data.success) {
          const type = slots.find(s => s.vehicleNumber === plate)?.vehicleType || 'Car';
          const rate = type === 'Car' ? 50 : 20;

          // 2. Call Billing Microservice on Port 8082 for detailed invoicing
          try {
            const billingRes = await fetch(`${BILLING_URL}/invoice`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                vehicleNumber: plate,
                vehicleType: type === 'Car' ? 'CAR' : 'BIKE',
                hoursParked: exitHours,
                hourlyRate: rate
              })
            });

            if (billingRes.ok) {
              const inv = await billingRes.json();
              
              // Load rich invoice into Modal state
              setReceipt({
                plate,
                type,
                hours: exitHours,
                rate,
                invoiceId: inv.invoiceId,
                subtotal: inv.subtotal,
                cgst: inv.cgst,
                sgst: inv.sgst,
                fee: inv.grandTotal, // Grand total containing CGST and SGST
                slotNumber: data.slotNumber,
                formattedReceipt: inv.formattedReceipt
              });

              addLog(`Invoice [${inv.invoiceId}] successfully generated on Billing Microservice (Port 8082).`, 'success');
              // Refresh invoice database
              fetchInvoices();
            } else {
              throw new Error("Invoicing failed");
            }
          } catch (billErr) {
            // Offline fallback
            addLog("Billing service unreachable. Generated flat invoice fallback.", "info");
            setReceipt({
              plate,
              type,
              hours: exitHours,
              rate,
              fee: data.fee,
              slotNumber: data.slotNumber
            });
          }

          setExitPlate('');
          setExitHours(1);
          await fetchData();
        } else {
          addLog(`Checkout failed: ${data.message}`, 'error');
        }
      } catch (err) {
        addLog("Communication failure with REST backend.", 'error');
      }
    } else {
      // Sandbox fallback
      const idx = slots.findIndex(s => s.occupied && s.vehicleNumber === plate);
      if (idx === -1) {
        addLog(`Checkout failed: vehicle [${plate}] not found.`, 'error');
        return;
      }

      const slot = slots[idx];
      const rate = slot.vehicleType === 'Car' ? 30 : 15;
      const fee = exitHours * rate;

      setReceipt({
        plate,
        type: slot.vehicleType,
        hours: exitHours,
        rate,
        fee,
        slotNumber: slot.slotNumber
      });

      const newSlots = [...slots];
      newSlots[idx] = {
        slotNumber: idx + 1,
        occupied: false,
        vehicleNumber: null,
        vehicleType: null,
        brand: null,
        entryTime: null
      };

      setSlots(newSlots);
      setOccupiedCount(prev => prev - 1);
      setVacantCount(prev => prev + 1);
      setRevenue(prev => prev + fee);
      addLog(`[Sandbox] Vehicle [${plate}] exited Slot #${slot.slotNumber}. Paid: $${fee}`, 'success');
      setExitPlate('');
      setExitHours(1);
    }
  };

  // Slot click helper
  const handleSlotClick = (slot) => {
    if (slot.occupied) {
      setExitPlate(slot.vehicleNumber);
      if (slot.entryTime) {
        try {
          const entry = new Date(slot.entryTime);
          const diffMs = new Date() - entry;
          const mins = diffMs / 60000;
          const hours = Math.max(1, Math.ceil(mins / 60));
          setExitHours(hours);
        } catch (e) {
          setExitHours(1);
        }
      }
    } else {
      setParkPlate(generateRandomPlate());
    }
  };

  // Render Login screen if not authenticated
  if (!userSession) {
    return (
      <>
        <div className="glow-bg glow-1"></div>
        <div className="glow-bg glow-2"></div>
        
        <div className="login-screen-overlay">
          <div className="login-card">
            <div className="login-header">
              <span className="login-logo">🔒</span>
              <h2 className="login-title">AutoPark Control</h2>
              <span className="login-subtitle">
                {isRegistering ? 'Create secure manager account' : 'Access smart parking dashboard'}
              </span>
            </div>

            {authError && <div className="login-error">{authError}</div>}
            {authSuccess && <div className="login-success-msg">{authSuccess}</div>}

            <form onSubmit={handleAuthSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div className="form-group">
                <label>Username</label>
                <input 
                  type="text" 
                  className="form-control" 
                  placeholder="Enter username" 
                  value={authUsername}
                  onChange={e => setAuthUsername(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>Password</label>
                <input 
                  type="password" 
                  className="form-control" 
                  placeholder="Enter password"
                  value={authPassword}
                  onChange={e => setAuthPassword(e.target.value)}
                  required
                />
              </div>

              {isRegistering && (
                <div className="form-group">
                  <label>Assigned Security Role</label>
                  <select 
                    className="form-control"
                    value={authRole}
                    onChange={e => setAuthRole(e.target.value)}
                  >
                    <option value="ROLE_ADMIN">Administrator (ROLE_ADMIN)</option>
                    <option value="ROLE_USER">Attendant (ROLE_USER)</option>
                  </select>
                </div>
              )}

              <button type="submit" className="btn-submit btn-primary">
                {isRegistering ? 'Register Account' : 'Authenticate Security'}
              </button>
            </form>

            <span className="login-toggle-text">
              {isRegistering ? 'Already registered?' : 'Need security access?'}
              <span 
                className="login-toggle-link" 
                style={{ marginLeft: '6px' }}
                onClick={() => {
                  setIsRegistering(!isRegistering);
                  setAuthError('');
                  setAuthSuccess('');
                }}
              >
                {isRegistering ? 'Login here' : 'Sign up here'}
              </span>
            </span>

            {/* Quick Demo Helper */}
            {!isRegistering && (
              <div className="demo-credentials-box">
                <span className="demo-title">💡 Developer Demo Account</span>
                <button type="button" className="demo-btn" onClick={prefillDemoCredentials}>
                  Auto-Fill credentials (User: admin / Pass: password123)
                </button>
              </div>
            )}
          </div>
        </div>
      </>
    );
  }

  // Render main dashboard when authenticated
  return (
    <>
      {/* Background glowing globes */}
      <div className="glow-bg glow-1"></div>
      <div className="glow-bg glow-2"></div>

      <div className="app-container">
        {/* Header */}
        <header className="app-header">
          <div className="logo-section">
            <span className="logo-icon">🚗</span>
            <span className="logo-text">AutoPark Cloud</span>
          </div>
          
          <div className="header-right">
            <div className={`api-badge ${isConnected ? 'connected' : 'disconnected'}`}>
              <span className="badge-dot"></span>
              <span>{isConnected ? 'Railway DB Connected' : 'Demo Sandbox Mode'}</span>
            </div>

            <button className="btn-registry" onClick={() => { fetchInvoices(); setShowInvoices(true); }}>
              📋 Invoices Registry ({invoices.length})
            </button>

            <div className="user-profile-widget">
              <span className="profile-avatar">
                {userSession.username.substring(0, 2).toUpperCase()}
              </span>
              <span className="profile-name">{userSession.username}</span>
            </div>

            <button className="btn-logout" onClick={handleLogout}>
              Sign Out
            </button>
          </div>
        </header>

        {/* Stats */}
        <section className="stats-container">
          <div className="glass-panel stat-card">
            <span className="stat-icon">📊</span>
            <div className="stat-details">
              <span className="stat-num">{slots.length || 20}</span>
              <span className="stat-label">Total capacity</span>
            </div>
          </div>
          <div className="glass-panel stat-card">
            <span className="stat-icon">🟢</span>
            <div className="stat-details">
              <span className="stat-num" style={{ color: 'var(--color-vacant)' }}>{vacantCount}</span>
              <span className="stat-label">Vacant Slots</span>
            </div>
          </div>
          <div className="glass-panel stat-card">
            <span className="stat-icon">🔴</span>
            <div className="stat-details">
              <span className="stat-num" style={{ color: 'var(--color-occupied)' }}>{occupiedCount}</span>
              <span className="stat-label">Occupied Slots</span>
            </div>
          </div>
          <div className="glass-panel stat-card">
            <span className="stat-icon" style={{ color: '#f59e0b' }}>💰</span>
            <div className="stat-details">
              <span className="stat-num" style={{ color: '#f59e0b' }}>
                {isConnected ? '₹' : '$'}{revenue}
              </span>
              <span className="stat-label">Total Revenue</span>
            </div>
          </div>
        </section>

        {/* Dashboard Grid */}
        <main className="dashboard-grid">
          {/* Left panel - Forms */}
          <div className="left-column">
            {/* Park vehicle form */}
            <div className="glass-panel form-card">
              <h3>Park a Vehicle</h3>
              <form onSubmit={handlePark}>
                <div className="form-group">
                  <label>License Plate Number</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="e.g. MH12AB1234"
                    value={parkPlate} 
                    onChange={e => setParkPlate(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Vehicle Type</label>
                  <div className="type-buttons">
                    <button 
                      type="button" 
                      className={`type-btn ${parkType === 'CAR' ? 'active' : ''}`}
                      onClick={() => setParkType('CAR')}
                    >
                      Car (₹50/h)
                    </button>
                    <button 
                      type="button" 
                      className={`type-btn ${parkType === 'BIKE' ? 'active' : ''}`}
                      onClick={() => setParkType('BIKE')}
                    >
                      Bike (₹20/h)
                    </button>
                  </div>
                </div>
                <button type="submit" className="btn btn-submit btn-primary">
                  Park Vehicle
                </button>
              </form>
            </div>

            {/* Exit vehicle form */}
            <div className="glass-panel form-card">
              <h3>Process Departure</h3>
              <form onSubmit={handleExit}>
                <div className="form-group">
                  <label>License Plate Number</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="Enter plate to exit"
                    value={exitPlate} 
                    onChange={e => setExitPlate(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Hours Parked: {exitHours}h</label>
                  <input 
                    type="range" 
                    min="1" 
                    max="24" 
                    className="form-control" 
                    style={{ padding: '0px', height: '14px' }}
                    value={exitHours}
                    onChange={e => setExitHours(parseInt(e.target.value))}
                  />
                </div>
                <button type="submit" className="btn btn-submit btn-success">
                  Release & Invoiced
                </button>
              </form>
            </div>
          </div>

          {/* Right panel - Slot grid map */}
          <div className="glass-panel map-panel">
            <div className="map-header">
              <h3>Real-Time Slot Map</h3>
              <p className="map-tip">💡 Click a slot to pre-fill checkout or park a car!</p>
            </div>

            <div className="slots-grid">
              {slots.map(slot => (
                <div 
                  key={slot.slotNumber} 
                  className={`slot-card ${slot.occupied ? 'occupied' : 'vacant'}`}
                  onClick={() => handleSlotClick(slot)}
                >
                  <span className="slot-num">SLOT #{slot.slotNumber}</span>
                  {slot.occupied ? (
                    <>
                      <span className="slot-status">OCCUPIED</span>
                      <span className="slot-plate">{slot.vehicleNumber}</span>
                      <span className="slot-details">{slot.brand || `${slot.vehicleType}`}</span>
                      <span className="slot-details" style={{ opacity: 0.6 }}>
                        In: {slot.entryTime && slot.entryTime.includes('T') 
                          ? new Date(slot.entryTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) 
                          : slot.entryTime?.substring(11) || '—'}
                      </span>
                    </>
                  ) : (
                    <>
                      <span className="slot-status">VACANT</span>
                      <span className="slot-details" style={{ opacity: 0.6 }}>Tap to Park</span>
                    </>
                  )}
                </div>
              ))}
            </div>
          </div>
        </main>

        {/* Logs */}
        <section className="glass-panel logs-panel">
          <span className="logs-title">System Activity Logs (MySQL Connection Broker)</span>
          <div className="logs-list">
            {logs.map((log, index) => (
              <div key={index} className="log-item">
                <span className="log-time">[{log.time}]</span>
                <span className={`log-text log-msg ${log.type === 'success' ? 'success' : log.type === 'error' ? 'error' : ''}`}>
                  {log.text}
                </span>
              </div>
            ))}
            <div ref={logsEndRef} />
          </div>
        </section>
      </div>

      {/* Modal invoice receipt */}
      {receipt && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ borderColor: 'var(--color-vacant)', maxWidth: '440px' }}>
            <div className="modal-check">✓</div>
            <span className="modal-title">DEPARTURE RECEIPT</span>
            
            {receipt.invoiceId && (
              <span style={{ fontSize: '11px', color: 'var(--accent-cyan)', fontWeight: '700', marginTop: '-10px' }}>
                INVOICE ID: {receipt.invoiceId}
              </span>
            )}

            {!showTerminal ? (
              <div className="modal-grid">
                <div className="modal-row">
                  <span className="modal-label">SLOT NUMBER:</span>
                  <span className="modal-val">#{receipt.slotNumber}</span>
                </div>
                <div className="modal-row">
                  <span className="modal-label">VEHICLE PLATE:</span>
                  <span className="modal-val">{receipt.plate}</span>
                </div>
                <div className="modal-row">
                  <span className="modal-label">HOURS PARKED:</span>
                  <span className="modal-val">{receipt.hours} Hours</span>
                </div>
                {receipt.subtotal !== undefined && (
                  <>
                    <div className="modal-row">
                      <span className="modal-label">SUBTOTAL:</span>
                      <span className="modal-val">₹{receipt.subtotal.toFixed(2)}</span>
                    </div>
                    <div className="modal-row">
                      <span className="modal-label">CGST (9.0%):</span>
                      <span className="modal-val">₹{receipt.cgst.toFixed(2)}</span>
                    </div>
                    <div className="modal-row">
                      <span className="modal-label">SGST (9.0%):</span>
                      <span className="modal-val">₹{receipt.sgst.toFixed(2)}</span>
                    </div>
                  </>
                )}
              </div>
            ) : (
              <pre className="terminal-receipt-box">
                {receipt.formattedReceipt}
              </pre>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <span className="modal-fee-label">Total Fee Paid</span>
              <span className="modal-fee">₹{receipt.fee.toFixed(2)}</span>
            </div>

            <div style={{ display: 'flex', gap: '10px', width: '100%' }}>
              {receipt.formattedReceipt && (
                <button 
                  className="btn-inspect" 
                  style={{ flex: 1, padding: '12px', fontSize: '12px', borderRadius: '12px' }}
                  onClick={() => setShowTerminal(!showTerminal)}
                >
                  {showTerminal ? '📋 Hide Terminal' : '📟 View Terminal'}
                </button>
              )}
              <button 
                className="btn btn-primary" 
                style={{ flex: 1, margin: 0, padding: '12px', borderRadius: '12px' }} 
                onClick={() => { setReceipt(null); setShowTerminal(false); }}
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Invoices Registry Modal */}
      {showInvoices && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ borderColor: '#10b981', maxWidth: '500px' }}>
            <div className="modal-check" style={{ backgroundColor: '#10b981', boxShadow: '0 0 15px rgba(16, 185, 129, 0.3)' }}>📋</div>
            <span className="modal-title" style={{ color: '#10b981' }}>INVOICES HISTORY REGISTRY</span>
            <span style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '-10px' }}>
              Fetched live from Billing Microservice on Port 8082
            </span>

            {selectedReceipt ? (
              <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <pre className="terminal-receipt-box" style={{ maxHeight: '250px', overflowY: 'auto' }}>
                  {selectedReceipt}
                </pre>
                <button 
                  className="btn-inspect" 
                  style={{ width: '100%', padding: '10px', borderRadius: '6px' }}
                  onClick={() => setSelectedReceipt(null)}
                >
                  ◀ Back to List
                </button>
              </div>
            ) : (
              <div className="invoice-list-container">
                {invoices.length === 0 ? (
                  <div style={{ textSelf: 'center', color: 'var(--text-muted)', padding: '20px', textAlign: 'center', fontSize: '13px' }}>
                    No invoices generated yet.
                  </div>
                ) : (
                  invoices.map(inv => (
                    <div key={inv.id} className="invoice-item-card">
                      <div className="invoice-item-info">
                        <span className="invoice-item-id">{inv.invoiceId}</span>
                        <span className="invoice-item-sub">
                          {inv.vehicleNumber} ({inv.vehicleType}) • {inv.hoursParked}h
                        </span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <span style={{ fontSize: '14px', fontWeight: '800', color: '#10b981' }}>
                          ₹{inv.grandTotal.toFixed(2)}
                        </span>
                        <button 
                          className="btn-inspect"
                          onClick={() => setSelectedReceipt(inv.formattedReceipt)}
                        >
                          Inspect 📟
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            <button 
              className="btn btn-primary" 
              style={{ width: '100%', backgroundColor: '#10b981', boxShadow: 'none' }}
              onClick={() => { setShowInvoices(false); setSelectedReceipt(null); }}
            >
              Close Registry
            </button>
          </div>
        </div>
      )}
    </>
  );
}

export default App;
