
const useEffect = React.useEffect;
const useRef = React.useRef;
const useState = React.useState;

function IndividualState({label, status, floor}) {
    return (
        <div className='elevator'>
            <h5>{label}</h5>
            <div>Status: {status}</div>
            <div>Floor: {floor}</div>
        </div>
    );
}

function CarState({carStates}) {
    return (
        <div className='floor'>
            <h3>Car Status</h3>
            {
                carStates.map(carState => <IndividualState label={carState.carName} status={carState.status} floor={carState.currentFloor} />)
            }
        </div>
    );
}

function Elevator({name, riderCue, submitInput, clearButton}) {

    const floorsRef = useRef(null);

    function handleClick() {
        if (floorsRef != null && floorsRef.current != null) {
            // attempt to parse and submit
            const floors = floorsRef.current.value.split(',').map(s => s.trim()).map(s => Number(s));
            console.log(floors);
            clearButton(riderCue.serviceRequest.direction);
            submitInput({
                riderServiceRequest: riderCue.serviceRequest,
                requestedFloors: floors
            });
        } else {
            console.log('the ref was bad');
        }
    }

    return (
        <div className='elevator'>
            <h5>{name}</h5>
            {   riderCue ? 
                    <div>
                        <input ref={floorsRef} className='elevator-components' type='text'></input>
                        <input onClick={handleClick} className='elevator-components' type='button' value='Go'></input>
                    </div> :
                    <span>Doors Closed</span>
            }
        </div>
    );
}

function Floor({floorNumber, numberOfFloors, elevators, riderCue, submitInput}) {

    const [upButtonState, setUpButtonState] = useState(floorNumber !== numberOfFloors);
    const [downButtonState, setDownButtonState] = useState(floorNumber !== 1);

    function handleClickBuilder(direction) {
        return () => {
            console.log(`${direction} clicked`);
            if ('ASCENDING' === direction) {
                setUpButtonState(false);
            } else {
                setDownButtonState(false);
            }
            submitInput({
                direction: direction,
                originationFloor: floorNumber
            });
        }
    }

    function clearButton(direction) {
        if ('ASCENDING' === direction) {
            setUpButtonState(floorNumber !== numberOfFloors);
        } else {
            setDownButtonState(floorNumber !== 1);
        }
    }

    if (riderCue) {
        console.log(`received rider cue for floor: ${floorNumber}`);
    }

    return (
        <div className='floor'>
            <h3>Floor {floorNumber}</h3>
            <div className='elevator'>
                <input disabled={!upButtonState} onClick={handleClickBuilder('ASCENDING')} className='elevator-components' type='button' value='Up'></input>
                <input disabled={!downButtonState} onClick={handleClickBuilder('DESCENDING')} className='elevator-components' type='button' value='Down'></input>
            </div>
            {
                elevators.map(elevator => {
                    const applicableRiderCue = riderCue && riderCue.carName === elevator.name ? riderCue : null;
                    return <Elevator name={elevator.name} riderCue={applicableRiderCue} submitInput={submitInput} clearButton={clearButton}/>
                })
            }
        </div>
    );
}

function App() {
    const [numberOfFloors, setNumberOfFloors] = useState();
    const [elevators, setElevators] = useState();
    const [riderCues, setRiderCues] = useState([]);
    const [submittedRiderCues, setSubmittedRiderCues] = useState([]);
    const [inputs, setInputs] = useState([]);
    const [carState, setCarStates] = useState([]);

    useEffect(() => {
        fetch('/configuration')
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                throw Error('connection to server failed');
            })
            .then(conf => {
                setNumberOfFloors(conf.totalFloors);
                setElevators(conf.elevatorNames.map(en => ({key: en, name: en})));
                setCarStates(conf.elevatorNames.map(en => ({carName: en, status: 'AVAILABLE', currentFloor: 0})));
            })
            .catch(err => alert(err));
    }, []);

    useEffect(() => {
        const evtSource = new EventSource('/service/events');
        evtSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log(`received message: ${event.data}`);
            if (data.serviceRequest) {
                setRiderCues(rcs => ([
                    ...rcs,
                    data
                ]));
            } else if (data.status) {
                setCarStates(cs => {
                    let affectedCarIdx;
                    for (let index = 0; index < cs.length; index++) {
                        if (cs[index].carName === data.carName) {
                            affectedCarIdx = index;
                            break;
                        }
                    }
                    const updatedCs = [ ...cs ];
                    updatedCs[affectedCarIdx] = data;
                    return updatedCs;
                });
            } else {
                console.log(data);
            }
        };
    }, []);

    useEffect(() => {

        if (inputs.length === 0) {
            return;
        }

        const latestInput = inputs[inputs.length - 1];

        if (latestInput.originationFloor) {
            fetch(
                '/service',
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(latestInput)
                }
            )
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                throw Error('unexpected server response');
            })
            .then(serviceRequest => console.log(`successfully submitted service request: ${JSON.stringify(serviceRequest)}`))
            .catch(err => alert(err));
        } else if (latestInput.riderServiceRequest) {
            setSubmittedRiderCues(src => ([
                ...src,
                latestInput.riderServiceRequest
            ]));
            fetch(
                `/service/${latestInput.riderServiceRequest.id}/floors`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(latestInput)
                }
            )
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                throw Error('unexpected server response');
            })
            .then(floorRequest => console.log(`successfully submitted floor request: ${JSON.stringify(floorRequest)}`))
            .catch(err => alert(err));
        } else {
            alert(`unidentified submission: ${latestInput}`);
        }

    }, [inputs]);

    function submitInput(input) {
        setInputs(inputs => ([
            ...inputs,
            input
        ]));
    }

    const floors = [];
    for (var i = 1; i <= numberOfFloors; i++) {
        const associatedCues = riderCues
            .filter(rc => {
                const correctFloor = rc.serviceRequest.originationFloor === i;
                const notPreviouslySubmitted = !submittedRiderCues.filter(src => rc.serviceRequest.id === src.id).length;
                return correctFloor && notPreviouslySubmitted;
            });
        let associatedCue = associatedCues.length === 0 ? null : associatedCues[0];
        floors.push(<Floor key={i} floorNumber={i} numberOfFloors={numberOfFloors} elevators={elevators} riderCue={associatedCue} submitInput={submitInput}/>);
    }

    return (
        <div>
            <CarState carStates={carState} />
            {floors.reverse()}
        </div>
    );
}

const container = document.getElementById('main');
const root = ReactDOM.createRoot(container);
root.render(<App />);
